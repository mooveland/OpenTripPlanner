package org.opentripplanner.raptor.rangeraptor.support;

import static org.opentripplanner.raptor.spi.RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;

import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.spi.TransitArrival;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleBoardSearch;

/**
 * This class contains code which is shared by all time-dependent routing strategies. It also
 * defines abstract methods, which the individual strategies must implement.
 */
public final class TimeBasedRoutingSupport<T extends RaptorTripSchedule> {

  private final SlackProvider slackProvider;
  private final TransitCalculator<T> calculator;
  private final RoundProvider roundProvider;
  private boolean inFirstIteration = true;
  private boolean hasTimeDependentAccess = false;
  private RaptorTimeTable<T> timeTable;
  private RaptorTripScheduleSearch<T> tripSearch;

  public TimeBasedRoutingSupport(
    SlackProvider slackProvider,
    TransitCalculator<T> calculator,
    RoundProvider roundProvider,
    WorkerLifeCycle subscriptions
  ) {
    this.slackProvider = slackProvider;
    this.calculator = calculator;
    this.roundProvider = roundProvider;

    subscriptions.onIterationComplete(() -> inFirstIteration = false);
  }

  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    this.timeTable = timeTable;
    this.tripSearch = createTripSearch(timeTable);
  }

  /**
   * Same as {@link #boardWithRegularTransfer(int, int, int, int, int)}, but with
   * {@code onTripIndex} unbounded.
   */
  public RaptorTripScheduleBoardOrAlightEvent<T> boardWithRegularTransfer(
    int prevArrivalTime,
    int stopPos,
    int boardSlack
  ) {
    return boardWithRegularTransfer(prevArrivalTime, stopPos, boardSlack, UNBOUNDED_TRIP_INDEX);
  }

  public RaptorTripScheduleBoardOrAlightEvent<T> boardWithRegularTransfer(
    int prevArrivalTime,
    int stopPos,
    int boardSlack,
    int onTripIndex
  ) {
    return searchForBoardingAfter(prevArrivalTime, stopPos, boardSlack, onTripIndex);
  }

  public RaptorTripScheduleBoardOrAlightEvent<T> searchForBoardingAfter(
    int prevArrivalTime,
    int stopPos,
    int boardSlack,
    int onTripIndex
  ) {
    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);
    return tripSearch.search(earliestBoardTime, stopPos, onTripIndex);
  }

  /**
   *
   * @param prevTransitStopArrival the current boarding previous transit arrival. This is used to
   *                               look up any guaranteed transfers.
   * @param prevArrivalTime        the arrival time for the board stop ({@code stopIndex}), this
   *                               may not be same as the {@code prevTransitStopArrival}, since
   *                               there might be a "walking" transfer to reach stop.
   * @param boardSlack             The minimum number of seconds to apply to the arrival time
   *                               before boarding a trip. Stay-seated and guaranteed transfers
   *                               may override this.
   * @param txSearch               The constrained transfer search to use.
   */
  public RaptorTripScheduleBoardOrAlightEvent<T> boardWithConstrainedTransfer(
    TransitArrival<T> prevTransitStopArrival,
    int prevArrivalTime,
    int boardSlack,
    RaptorConstrainedTripScheduleBoardingSearch<T> txSearch
  ) {
    // Get the previous transit stop arrival (transfer source)
    if (prevTransitStopArrival == null) {
      return RaptorTripScheduleBoardOrAlightEvent.empty(
        earliestBoardTime(prevArrivalTime, boardSlack)
      );
    }

    int prevTransitStopArrivalTime = prevTransitStopArrival.arrivalTime();

    int prevTransitArrivalTime = calculator.minusDuration(
      prevTransitStopArrivalTime,
      slackProvider.alightSlack(prevTransitStopArrival.trip().pattern().slackIndex())
    );

    int earliestBoardTime = earliestBoardTime(prevArrivalTime, boardSlack);

    return txSearch.find(
      timeTable,
      slackProvider.transferSlack(),
      prevTransitStopArrival.trip(),
      prevTransitStopArrival.stop(),
      prevTransitArrivalTime,
      earliestBoardTime
    );
  }

  /**
   * Get the time-dependent departure time for an access/egress and mark if we have time-dependent
   * accesses or egresses
   */
  public int getTimeDependentDepartureTime(RaptorAccessEgress it, int iterationDepartureTime) {
    // Earliest possible departure time from the origin, or latest possible arrival
    // time at the destination if searching backwards.
    int timeDependentDepartureTime = calculator.departureTime(it, iterationDepartureTime);

    // This access is not available after the iteration departure time
    if (timeDependentDepartureTime == -1) {
      return -1;
    }

    // If the time differs from the iterationDepartureTime, then the access has time
    // restrictions. If the difference between _any_ access between iterations is not a
    // uniform iterationStep, then the exactTripSearch optimisation may not be used.
    if (timeDependentDepartureTime != iterationDepartureTime) {
      hasTimeDependentAccess = true;
    }

    return timeDependentDepartureTime;
  }

  /**
   * Add board-slack(forward-search) or alight-slack(reverse-search)
   */
  private int earliestBoardTime(int prevArrivalTime, int boardSlack) {
    return calculator.plusDuration(prevArrivalTime, boardSlack);
  }

  /**
   * Create a trip search using {@link TripScheduleBoardSearch}.
   */
  private RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
    if (!inFirstIteration && roundProvider.isFirstRound() && !hasTimeDependentAccess) {
      // For the first round of every iteration(except the first) we restrict the first
      // departure to happen within the time-window of the iteration. Another way to put this,
      // is to say that we allow for the access path to be time-shifted to a later departure,
      // but not past the previous iteration departure time. This save a bit of processing,
      // but most importantly allow us to use the departure-time as a pareto criteria in
      // time-table view. This is not valid for the first iteration, because we could jump on
      // a bus, take it one stop and walk back and then wait to board a later trip - this kind
      // of results would be rejected by earlier iterations, for all iterations except the
      // first.
      return calculator.createExactTripSearch(timeTable);
    }

    // Default: create a standard trip search
    return calculator.createTripSearch(timeTable);
  }
}