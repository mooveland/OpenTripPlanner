package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;

public class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private static final int DAY_IN_SECONDS = 3600 * 24;

    private String currentHeadsign;

    public void mapTripPattern(JourneyPattern journeyPattern, OtpTransitBuilder transitBuilder, NetexDao netexDao) {
        TripMapper tripMapper = new TripMapper();

        List<Trip> trips = new ArrayList<>();

        //find matching journey pattern
        Collection<ServiceJourney> serviceJourneys = netexDao.serviceJourneyByPatternId.lookup(journeyPattern.getId());

        StopPattern stopPattern = null;

        Route route = netexDao.routeById.lookup(journeyPattern.getRouteRef().getRef());
        org.opentripplanner.model.Route otpRoute = transitBuilder.getRoutes()
                .get(AgencyAndIdFactory.createAgencyAndId(route.getLineRef().getValue().getRef()));

        if (serviceJourneys == null || serviceJourneys.isEmpty()) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId() + " does not contain any serviceJourneys.");
            return;
        }

        for (ServiceJourney serviceJourney : serviceJourneys) {
            Trip trip = tripMapper.mapServiceJourney(serviceJourney, transitBuilder, netexDao);
            trips.add(trip);

            TimetabledPassingTimes_RelStructure passingTimes = serviceJourney.getPassingTimes();
            List<TimetabledPassingTime> timetabledPassingTimes = passingTimes.getTimetabledPassingTime();

            List<StopTime> stopTimes = mapToStopTimes(
                    journeyPattern, transitBuilder, netexDao, trip, timetabledPassingTimes
            );

            if (stopTimes != null && stopTimes.size() > 0) {
                transitBuilder.getStopTimesSortedByTrip().put(trip, stopTimes);

                List<StopTime> stopTimesWithHeadsign = stopTimes.stream()
                        .filter(s -> s.getStopHeadsign() != null && s.getStopHeadsign() != "")
                        .collect(Collectors.toList());

                // Set first non-empty headsign as trip headsign
                if (stopTimesWithHeadsign.size() > 0) {
                    trip.setTripHeadsign(stopTimesWithHeadsign.stream()
                            .sorted(Comparator.comparingInt(StopTime::getStopSequence)).findFirst()
                            .get().getStopHeadsign());
                }
                else {
                    trip.setTripHeadsign("");
                }

                // We only generate a stopPattern for the first trip in the JourneyPattern.
                // We can do this because we assume the stopPatterrns are the same for all trips in a
                // JourneyPattern
                if (stopPattern == null) {
                    stopPattern = new StopPattern(transitBuilder.getStopTimesSortedByTrip().get(trip));
                }
            }
            else {
                LOG.warn("No stop times found for trip " + serviceJourney.getId());
            }
        }

        if (stopPattern == null || stopPattern.size < 2) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId()
                    + " does not contain a valid stop pattern.");
            return;
        }

        TripPattern tripPattern = new TripPattern(otpRoute, stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name = journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();
        tripPattern.id = AgencyAndIdFactory.createAgencyAndId(journeyPattern.getId());

        Deduplicator deduplicator = new Deduplicator();

        for (Trip trip : trips) {
            if (transitBuilder.getStopTimesSortedByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip,
                        transitBuilder.getStopTimesSortedByTrip().get(trip), deduplicator);
                tripPattern.add(tripTimes);
                transitBuilder.getTrips().add(trip);
            }
        }

        transitBuilder.getTripPatterns().put(tripPattern.stopPattern, tripPattern);
    }

    private List<StopTime> mapToStopTimes(JourneyPattern journeyPattern, OtpTransitBuilder transitBuilder, NetexDao netexDao, Trip trip, List<TimetabledPassingTime> timetabledPassingTimes) {
        List<StopTime> stopTimes = new ArrayList<>();

        int stopSequence = 0;

        for (TimetabledPassingTime passingTime : timetabledPassingTimes) {
            JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRef
                    = passingTime.getPointInJourneyPatternRef();
            String ref = pointInJourneyPatternRef.getValue().getRef();

            Stop quay = findQuay(ref, journeyPattern, netexDao, transitBuilder);

            if (quay != null) {
                StopPointInJourneyPattern stopPoint = findStopPoint(ref, journeyPattern);
                StopTime stopTime = mapToStopTime(trip, stopPoint, quay, passingTime, stopSequence, netexDao);

                if (stopTimes.size() > 0 && stopTimeNegative(stopTimes.get(stopTimes.size() - 1), stopTime)) {
                    LOG.error("Stoptime increased by negative amount in serviceJourney " + trip.getId().getId());
                    return null;
                }

                stopTimes.add(stopTime);
                ++stopSequence;
            } else {
                LOG.warn("Quay not found for timetabledPassingTimes: " + passingTime.getId());
            }
        }
        return stopTimes;
    }

    private boolean stopTimeNegative(StopTime stopTime1, StopTime stopTime2) {
        int time1 = Math.max(stopTime1.getArrivalTime(), stopTime1.getDepartureTime());
        int time2 = Math.max(stopTime2.getArrivalTime(), stopTime2.getDepartureTime());

        return !(time1 >= 0 && time2 >= 0 && time2 >= time1);
    }

    private StopTime mapToStopTime(Trip trip, StopPointInJourneyPattern stopPoint, Stop quay,
                                   TimetabledPassingTime passingTime, int stopSequence, NetexDao netexDao) {
        StopTime stopTime = new StopTime();
        stopTime.setId(AgencyAndIdFactory.createAgencyAndId(passingTime.getId()));
        stopTime.setTrip(trip);
        stopTime.setStopSequence(stopSequence);
        stopTime.setStop(quay);

        stopTime.setArrivalTime(
                calculateOtpTime(passingTime.getArrivalTime(), passingTime.getArrivalDayOffset(),
                        passingTime.getDepartureTime(), passingTime.getDepartureDayOffset()));

        stopTime.setDepartureTime(calculateOtpTime(passingTime.getDepartureTime(),
                passingTime.getDepartureDayOffset(), passingTime.getArrivalTime(),
                passingTime.getArrivalDayOffset()));

        if (stopPoint != null) {
            if (isFalse(stopPoint.isForAlighting())) {
                stopTime.setDropOffType(PICKDROP_NONE);
            } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
                stopTime.setDropOffType(PICKDROP_COORDINATE_WITH_DRIVER);
            } else {
                stopTime.setDropOffType(PICKDROP_SCHEDULED);
            }

            if (isFalse(stopPoint.isForBoarding())) {
                stopTime.setPickupType(PICKDROP_NONE);
            } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
                stopTime.setPickupType(PICKDROP_COORDINATE_WITH_DRIVER);
            } else {
                stopTime.setPickupType(PICKDROP_SCHEDULED);
            }
        }

        if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
            LOG.warn("Time missing for trip " + trip.getId());
        }

        if (stopPoint.getDestinationDisplayRef() != null) {
            DestinationDisplay value = netexDao.destinationDisplayById.lookup(stopPoint.getDestinationDisplayRef().getRef());
            if (value != null) {
                currentHeadsign = value.getFrontText().getValue();
            }
        }

        if (currentHeadsign != null) {
            stopTime.setStopHeadsign(currentHeadsign);
        }

        return stopTime;
    }

    private Stop findQuay(String pointInJourneyPatterRef, JourneyPattern journeyPattern,
            NetexDao netexDao, OtpTransitBuilder transitBuilder) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if (stop.getId().equals(pointInJourneyPatterRef)) {
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point)
                            .getScheduledStopPointRef();
                    String stopId = netexDao.quayIdByStopPointRef.lookup(scheduledStopPointRef.getValue().getRef());
                    if (stopId == null) {
                        LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef
                                .getValue().getRef());
                    } else {
                        Stop quay = transitBuilder.getStops()
                                .get(AgencyAndIdFactory.createAgencyAndId(stopId));
                        if (quay == null) {
                            LOG.warn("Quay not found for " + scheduledStopPointRef.getValue()
                                    .getRef());
                        }
                        return quay;
                    }
                }
            }
        }

        return null;
    }

    private StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef,
            JourneyPattern journeyPattern) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) point;
                if (stopPoint.getId().equals(pointInJourneyPatterRef)) {
                    return stopPoint;
                }
            }
        }
        return null;
    }

    private static int calculateOtpTime(LocalTime time, BigInteger dayOffset,
            LocalTime fallbackTime, BigInteger fallbackDayOffset) {
        return time != null ?
                calculateOtpTime(time, dayOffset) :
                calculateOtpTime(fallbackTime, fallbackDayOffset);
    }

    static int calculateOtpTime(LocalTime time, BigInteger dayOffset) {
        int otpTime = time.toSecondOfDay();
        if (dayOffset != null) {
            otpTime += DAY_IN_SECONDS * dayOffset.intValue();
        }
        return otpTime;
    }

    private boolean isFalse(Boolean value) {
        return value != null && !value;
    }
}