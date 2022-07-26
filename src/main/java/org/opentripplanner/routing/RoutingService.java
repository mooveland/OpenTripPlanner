package org.opentripplanner.routing;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.standalone.api.OtpServerContext;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.WorldEnvelope;

/**
 * Entry point for requests towards the routing API.
 */
public class RoutingService {

  private final Graph graph;

  private final TransitService transitService;

  private final GraphFinder graphFinder;

  public RoutingService(Graph graph, TransitService transitService) {
    this.graph = graph;
    this.transitService = transitService;
    this.graphFinder = GraphFinder.getInstance(graph);
  }

  // TODO We should probably not have the Router as a parameter here
  public RoutingResponse route(RoutingRequest request, OtpServerContext serverContext) {
    RoutingWorker worker = new RoutingWorker(serverContext, request, transitService.getTimeZone());
    return worker.route();
  }

  /** {@link Graph#addVertex(Vertex)} */
  public void addVertex(Vertex v) {
    this.graph.addVertex(v);
  }

  /** {@link Graph#removeEdge(Edge)} */
  public void removeEdge(Edge e) {
    this.graph.removeEdge(e);
  }

  /** {@link Graph#getVertex(String)} */
  public Vertex getVertex(String label) {
    return this.graph.getVertex(label);
  }

  /** {@link Graph#getVertices()} */
  public Collection<Vertex> getVertices() {
    return this.graph.getVertices();
  }

  /** {@link Graph#getVerticesOfType(Class)} */
  public <T extends Vertex> List<T> getVerticesOfType(Class<T> cls) {
    return this.graph.getVerticesOfType(cls);
  }

  /** {@link Graph#getEdges()} */
  public Collection<Edge> getEdges() {
    return this.graph.getEdges();
  }

  /** {@link Graph#getEdgesOfType(Class)} */
  public <T extends Edge> List<T> getEdgesOfType(Class<T> cls) {
    return this.graph.getEdgesOfType(cls);
  }

  /** {@link Graph#getStreetEdges()} */
  public Collection<StreetEdge> getStreetEdges() {
    return this.graph.getStreetEdges();
  }

  /** {@link Graph#containsVertex(Vertex)} */
  public boolean containsVertex(Vertex v) {
    return this.graph.containsVertex(v);
  }

  /** {@link Graph#putService(Class, Serializable)} */
  public <T extends Serializable> T putService(Class<T> serviceType, T service) {
    return this.graph.putService(serviceType, service);
  }

  /** {@link Graph#hasService(Class)} */
  public boolean hasService(Class<? extends Serializable> serviceType) {
    return this.graph.hasService(serviceType);
  }

  /** {@link Graph#getService(Class)} */
  public <T extends Serializable> T getService(Class<T> serviceType) {
    return this.graph.getService(serviceType);
  }

  /** {@link Graph#getService(Class, boolean)} */
  public <T extends Serializable> T getService(Class<T> serviceType, boolean autoCreate) {
    return this.graph.getService(serviceType, autoCreate);
  }

  /** {@link Graph#remove(Vertex)} */
  public void remove(Vertex vertex) {
    this.graph.remove(vertex);
  }

  /** {@link Graph#removeIfUnconnected(Vertex)} */
  public void removeIfUnconnected(Vertex v) {
    this.graph.removeIfUnconnected(v);
  }

  /** {@link Graph#getExtent()} */
  public Envelope getExtent() {
    return this.graph.getExtent();
  }

  /** {@link Graph#getBundle()} */
  public GraphBundle getBundle() {
    return this.graph.getBundle();
  }

  /** {@link Graph#setBundle(GraphBundle)} */
  public void setBundle(GraphBundle bundle) {
    this.graph.setBundle(bundle);
  }

  /** {@link Graph#countVertices()} */
  public int countVertices() {
    return this.graph.countVertices();
  }

  /** {@link Graph#countEdges()} */
  public int countEdges() {
    return this.graph.countEdges();
  }

  /** {@link Graph#getStreetIndex()} */
  public StreetVertexIndex getStreetIndex() {
    return this.graph.getStreetIndex();
  }

  /** {@link Graph#getLinker()} */
  public VertexLinker getLinker() {
    return this.graph.getLinker();
  }

  /** {@link Graph#removeEdgelessVertices()} */
  public int removeEdgelessVertices() {
    return this.graph.removeEdgelessVertices();
  }

  /** {@link Graph#calculateEnvelope()} */
  public void calculateEnvelope() {
    this.graph.calculateEnvelope();
  }

  /** {@link Graph#calculateConvexHull()} */
  public void calculateConvexHull() {
    this.graph.calculateConvexHull();
  }

  /** {@link Graph#getConvexHull()} */
  public Geometry getConvexHull() {
    return this.graph.getConvexHull();
  }

  /** {@link Graph#expandToInclude(double, double)} ()} */
  public void expandToInclude(double x, double y) {
    this.graph.expandToInclude(x, y);
  }

  /** {@link Graph#getEnvelope()} */
  public WorldEnvelope getEnvelope() {
    return this.graph.getEnvelope();
  }

  /** {@link Graph#getDistanceBetweenElevationSamples()} */
  public double getDistanceBetweenElevationSamples() {
    return this.graph.getDistanceBetweenElevationSamples();
  }

  /** {@link Graph#setDistanceBetweenElevationSamples(double)} */
  public void setDistanceBetweenElevationSamples(double distanceBetweenElevationSamples) {
    this.graph.setDistanceBetweenElevationSamples(distanceBetweenElevationSamples);
  }

  public RealtimeVehiclePositionService getVehiclePositionService() {
    return this.graph.getVehiclePositionService();
  }

  /** {@link Graph#getVehicleRentalStationService()} */
  public VehicleRentalStationService getVehicleRentalStationService() {
    return this.graph.getVehicleRentalStationService();
  }

  /** {@link Graph#getVehicleParkingService()} */
  public VehicleParkingService getVehicleParkingService() {
    return this.graph.getVehicleParkingService();
  }

  /** {@link Graph#getDrivingDirection()} */
  public DrivingDirection getDrivingDirection() {
    return this.graph.getDrivingDirection();
  }

  /** {@link Graph#setDrivingDirection(DrivingDirection)} */
  public void setDrivingDirection(DrivingDirection drivingDirection) {
    this.graph.setDrivingDirection(drivingDirection);
  }

  /** {@link Graph#getIntersectionTraversalModel()} */
  public IntersectionTraversalCostModel getIntersectionTraversalModel() {
    return this.graph.getIntersectionTraversalModel();
  }

  /** {@link Graph#setIntersectionTraversalCostModel(IntersectionTraversalCostModel)} */
  public void setIntersectionTraversalCostModel(
    IntersectionTraversalCostModel intersectionTraversalCostModel
  ) {
    this.graph.setIntersectionTraversalCostModel(intersectionTraversalCostModel);
  }

  /** {@link GraphFinder#findClosestStops(double, double, double)} */
  public List<NearbyStop> findClosestStops(double lat, double lon, double radiusMeters) {
    return this.graphFinder.findClosestStops(lat, lon, radiusMeters);
  }

  /**
   * {@link GraphFinder#findClosestPlaces(double, double, double, int, List, List, List, List, List,
   * RoutingService, TransitService)}
   */
  public List<PlaceAtDistance> findClosestPlaces(
    double lat,
    double lon,
    double radiusMeters,
    int maxResults,
    List<TransitMode> filterByModes,
    List<PlaceType> filterByPlaceTypes,
    List<FeedScopedId> filterByStops,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    List<String> filterByBikeParks,
    List<String> filterByCarParks,
    RoutingService routingService,
    TransitService transitService
  ) {
    return this.graphFinder.findClosestPlaces(
        lat,
        lon,
        radiusMeters,
        maxResults,
        filterByModes,
        filterByPlaceTypes,
        filterByStops,
        filterByRoutes,
        filterByBikeRentalStations,
        routingService,
        transitService
      );
  }

  /** {@link Graph#getStopsByBoundingBox(double, double, double, double)} */
  public Collection<StopLocation> getStopsByBoundingBox(
    double minLat,
    double minLon,
    double maxLat,
    double maxLon
  ) {
    return this.graph.getStopsByBoundingBox(minLat, minLon, maxLat, maxLon);
  }

  /** {@link Graph#getStopsInRadius(WgsCoordinate, double)} */
  public List<T2<Stop, Double>> getStopsInRadius(WgsCoordinate center, double radius) {
    return this.graph.getStopsInRadius(center, radius);
  }
}
