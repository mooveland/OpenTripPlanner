package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
    RouteRequest request,
    TemporaryVerticesContainer verticesContainer,
    OtpServerRequestContext serverContext,
    AdditionalSearchDays searchDays,
    FlexParameters params,
    DataOverlayContext dataOverlayContext,
    boolean isEgress
  ) {
    TransitService transitService = serverContext.transitService();

    Collection<NearbyStop> accessStops = !isEgress
      ? AccessEgressRouter.streetSearch(
        request,
        verticesContainer,
        transitService,
        new StreetRequest(StreetMode.WALK),
        dataOverlayContext,
        false
      )
      : List.of();

    Collection<NearbyStop> egressStops = isEgress
      ? AccessEgressRouter.streetSearch(
        request,
        verticesContainer,
        transitService,
        new StreetRequest(StreetMode.WALK),
        dataOverlayContext,
        true
      )
      : List.of();

    FlexRouter flexRouter = new FlexRouter(
      serverContext.graph(),
      transitService,
      params,
      request.dateTime(),
      request.arriveBy(),
      searchDays.additionalSearchDaysInPast(),
      searchDays.additionalSearchDaysInFuture(),
      accessStops,
      egressStops
    );

    return isEgress ? flexRouter.createFlexEgresses() : flexRouter.createFlexAccesses();
  }
}
