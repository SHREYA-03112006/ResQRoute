package com.resqroute;

import com.resqroute.model.RoadEdge;
import com.resqroute.model.RoadNode;
import com.resqroute.model.RoadStatus;
import com.resqroute.routing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RoutingEngineTest {

    private DijkstraRouter dijkstraRouter;
    private AStarRouter aStarRouter;
    private AlternativeRoutesGenerator alternativeRoutesGenerator;

    private RoadNode nodeA;
    private RoadNode nodeB;
    private RoadNode nodeC;
    private RoadNode nodeD;

    @BeforeEach
    void setUp() {
        dijkstraRouter = new DijkstraRouter();
        aStarRouter = new AStarRouter();
        alternativeRoutesGenerator = new AlternativeRoutesGenerator(dijkstraRouter, aStarRouter);

        nodeA = RoadNode.builder().id(1L).latitude(30.0).longitude(78.0).name("Node A").build();
        nodeB = RoadNode.builder().id(2L).latitude(30.1).longitude(78.1).name("Node B").build();
        nodeC = RoadNode.builder().id(3L).latitude(30.2).longitude(78.2).name("Node C").build();
        nodeD = RoadNode.builder().id(4L).latitude(30.3).longitude(78.3).name("Node D").build();
    }

    @Test
    @DisplayName("Dijkstra finds shortest path across direct route")
    void testDijkstraDirectRoute() {
        RoadGraph graph = new RoadGraph();
        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);

        RoadEdge edgeAB = RoadEdge.builder()
            .id(101L).fromNode(nodeA).toNode(nodeB)
            .distanceKm(10.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();
        RoadEdge edgeBC = RoadEdge.builder()
            .id(102L).fromNode(nodeB).toNode(nodeC)
            .distanceKm(10.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();

        graph.addEdge(edgeAB, 1.0);
        graph.addEdge(edgeBC, 1.0);

        Optional<RouteResult> result = dijkstraRouter.findRoute(graph, 1L, 3L);
        assertTrue(result.isPresent());
        assertEquals(20.0, result.get().totalDistanceKm());
        assertEquals(30.0, result.get().estimatedTimeMinutes());
        assertEquals(List.of(1L, 2L, 3L), result.get().nodeIds());
    }

    @Test
    @DisplayName("Dijkstra avoids BLOCKED road and takes detour")
    void testDijkstraAvoidsBlockedRoad() {
        RoadGraph graph = new RoadGraph();
        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);
        graph.addNode(nodeD);

        // Path 1: A -> B -> D (direct, but B -> D is BLOCKED)
        RoadEdge edgeAB = RoadEdge.builder()
            .id(101L).fromNode(nodeA).toNode(nodeB)
            .distanceKm(5.0).baseTravelTimeMinutes(10.0).status(RoadStatus.OPEN).bidirectional(true).build();
        RoadEdge edgeBD = RoadEdge.builder()
            .id(102L).fromNode(nodeB).toNode(nodeD)
            .distanceKm(5.0).baseTravelTimeMinutes(10.0).status(RoadStatus.BLOCKED).bidirectional(true).build();

        // Path 2: A -> C -> D (detour, OPEN)
        RoadEdge edgeAC = RoadEdge.builder()
            .id(103L).fromNode(nodeA).toNode(nodeC)
            .distanceKm(8.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();
        RoadEdge edgeCD = RoadEdge.builder()
            .id(104L).fromNode(nodeC).toNode(nodeD)
            .distanceKm(8.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();

        graph.addEdge(edgeAB, 1.0);
        graph.addEdge(edgeBD, 1.0);
        graph.addEdge(edgeAC, 1.0);
        graph.addEdge(edgeCD, 1.0);

        Optional<RouteResult> result = dijkstraRouter.findRoute(graph, 1L, 4L);
        assertTrue(result.isPresent());
        // Must choose detour A -> C -> D
        assertEquals(List.of(1L, 3L, 4L), result.get().nodeIds());
        assertEquals(16.0, result.get().totalDistanceKm());
    }

    @Test
    @DisplayName("A* router produces valid optimal route")
    void testAStarRouter() {
        RoadGraph graph = new RoadGraph();
        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);

        RoadEdge edgeAB = RoadEdge.builder()
            .id(101L).fromNode(nodeA).toNode(nodeB)
            .distanceKm(10.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();
        RoadEdge edgeBC = RoadEdge.builder()
            .id(102L).fromNode(nodeB).toNode(nodeC)
            .distanceKm(10.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();

        graph.addEdge(edgeAB, 1.0);
        graph.addEdge(edgeBC, 1.0);

        Optional<RouteResult> result = aStarRouter.findRoute(graph, 1L, 3L);
        assertTrue(result.isPresent());
        assertEquals("A_STAR", result.get().algorithm());
        assertEquals(20.0, result.get().totalDistanceKm());
        assertEquals(List.of(1L, 2L, 3L), result.get().nodeIds());
    }

    @Test
    @DisplayName("Alternative routes generator returns multiple distinct routes")
    void testAlternativeRoutes() {
        RoadEdge edgeAB = RoadEdge.builder()
            .id(101L).fromNode(nodeA).toNode(nodeB)
            .distanceKm(5.0).baseTravelTimeMinutes(10.0).status(RoadStatus.OPEN).bidirectional(true).build();
        RoadEdge edgeBD = RoadEdge.builder()
            .id(102L).fromNode(nodeB).toNode(nodeD)
            .distanceKm(5.0).baseTravelTimeMinutes(10.0).status(RoadStatus.OPEN).bidirectional(true).build();

        RoadEdge edgeAC = RoadEdge.builder()
            .id(103L).fromNode(nodeA).toNode(nodeC)
            .distanceKm(8.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();
        RoadEdge edgeCD = RoadEdge.builder()
            .id(104L).fromNode(nodeC).toNode(nodeD)
            .distanceKm(8.0).baseTravelTimeMinutes(15.0).status(RoadStatus.OPEN).bidirectional(true).build();

        List<RoadNode> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<RoadEdge> edges = List.of(edgeAB, edgeBD, edgeAC, edgeCD);

        List<RouteResult> routes = alternativeRoutesGenerator.generateRoutes(nodes, edges, 1L, 4L, "DIJKSTRA", 2);
        assertTrue(routes.size() >= 2);
        assertNotEquals(routes.get(0).nodeIds(), routes.get(1).nodeIds());
    }
}
