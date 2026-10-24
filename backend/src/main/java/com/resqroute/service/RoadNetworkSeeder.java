package com.resqroute.service;

import com.resqroute.model.RoadEdge;
import com.resqroute.model.RoadNode;
import com.resqroute.model.RoadStatus;
import com.resqroute.repository.RoadEdgeRepository;
import com.resqroute.repository.RoadNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Seeds the OpenStreetMap road graph for the Chamoli & Rudraprayag pilot district.
 *
 * <p>Represents the real-world road corridors of the Garhwal Himalayas:
 * <ul>
 *   <li><b>NH-7 (Alaknanda Corridor):</b> Devprayag - Srinagar - Rudraprayag - Gauchar - Karnaprayag - Nandaprayag - Chamoli - Pipalkoti - Helang - Joshimath - Badrinath</li>
 *   <li><b>NH-107 (Mandakini Corridor):</b> Rudraprayag - Tilwara - Agastmuni - Kund - Guptkashi - Phata - Sonprayag - Gaurikund</li>
 *   <li><b>Chopta Pass Link (Mountain Bypass):</b> Kund - Ukhimath - Chopta Pass - Mandal - Gopeshwar - Chamoli</li>
 *   <li><b>Connecting Links:</b> Depot, camp, and hospital spurs</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoadNetworkSeeder {

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;

    @Transactional
    public void seedRoadNetworkIfEmpty() {
        if (roadNodeRepository.count() > 0) {
            log.debug("Road network already seeded ({} nodes), skipping.", roadNodeRepository.count());
            return;
        }

        log.info("Seeding OpenStreetMap road network for Chamoli & Rudraprayag pilot district...");

        // 1. Create Nodes
        Map<String, RoadNode> nodeMap = new HashMap<>();

        // NH-7 Corridor Nodes
        addNode(nodeMap, "DEVPRAYAG", 30.146, 78.599, "Devprayag Junction (NH-7)", 1001L);
        addNode(nodeMap, "SRINAGAR", 30.224, 78.791, "Srinagar Garhwal (NH-7)", 1002L);
        addNode(nodeMap, "KIRTINAGAR", 30.237, 78.824, "Kirtinagar Bridge (NH-7)", 1003L);
        addNode(nodeMap, "RUDRAPRAYAG_JN", 30.285, 78.981, "Rudraprayag Confluence (NH-7 / NH-107)", 1004L);
        addNode(nodeMap, "GAUCHAR", 30.288, 79.155, "Gauchar Airstrip (NH-7)", 1005L);
        addNode(nodeMap, "KARNAPRAYAG", 30.260, 79.219, "Karnaprayag Confluence (NH-7 / NH-109)", 1006L);
        addNode(nodeMap, "NANDAPRAYAG", 30.331, 79.324, "Nandaprayag (NH-7)", 1007L);
        addNode(nodeMap, "CHAMOLI_JN", 30.408, 79.348, "Chamoli Junction (NH-7)", 1008L);
        addNode(nodeMap, "GOPESHWAR", 30.396, 79.321, "Gopeshwar Town (Dist Headquarters)", 1009L);
        addNode(nodeMap, "PIPALKOTI", 30.473, 79.479, "Pipalkoti (NH-7)", 1010L);
        addNode(nodeMap, "HELANG", 30.528, 79.520, "Helang (NH-7)", 1011L);
        addNode(nodeMap, "JOSHIMATH", 30.556, 79.567, "Joshimath Gateway (NH-7)", 1012L);
        addNode(nodeMap, "GOVINDGHAT", 30.624, 79.596, "Govindghat (NH-7)", 1013L);
        addNode(nodeMap, "PANDUKESHWAR", 30.665, 79.592, "Pandukeshwar (NH-7)", 1014L);
        addNode(nodeMap, "BADRINATH", 30.743, 79.493, "Badrinath (NH-7 Terminus)", 1015L);

        // NH-107 Mandakini Corridor Nodes
        addNode(nodeMap, "TILWARA", 30.292, 78.962, "Tilwara (NH-107)", 1020L);
        addNode(nodeMap, "AGASTMUNI", 30.393, 79.030, "Agastmuni (NH-107)", 1021L);
        addNode(nodeMap, "KUND_JN", 30.505, 79.112, "Kund Junction (NH-107 / Chopta Link)", 1022L);
        addNode(nodeMap, "GUPTKASHI", 30.523, 79.079, "Guptkashi (NH-107)", 1023L);
        addNode(nodeMap, "PHATA", 30.575, 79.038, "Phata Helipad (NH-107)", 1024L);
        addNode(nodeMap, "SONPRAYAG", 30.631, 78.998, "Sonprayag (NH-107)", 1025L);
        addNode(nodeMap, "GAURIKUND", 30.645, 79.266, "Gaurikund Base (NH-107)", 1026L);

        // Chopta Mountain Bypass Corridor Nodes
        addNode(nodeMap, "UKHIMATH", 30.518, 79.098, "Ukhimath (SH Link)", 1030L);
        addNode(nodeMap, "CHOPTA_PASS", 30.485, 79.172, "Chopta Pass (Tungnath base)", 1031L);
        addNode(nodeMap, "MANDAL", 30.457, 79.277, "Mandal Valley (SH Link)", 1032L);

        // Southern Connector Nodes
        addNode(nodeMap, "THARALI", 30.063, 79.494, "Tharali (NH-109 southern link)", 1040L);

        roadNodeRepository.saveAll(nodeMap.values());
        log.info("Saved {} road nodes.", nodeMap.size());

        // 2. Create Edges
        List<RoadEdge> edges = new ArrayList<>();

        // NH-7 Corridor Segments
        addEdge(edges, nodeMap, "DEVPRAYAG", "SRINAGAR", "NH-7 (Devprayag-Srinagar)", "NH", 34.0, 50.0, RoadStatus.OPEN, 5001L);
        addEdge(edges, nodeMap, "SRINAGAR", "KIRTINAGAR", "NH-7 (Srinagar-Kirtinagar)", "NH", 5.0, 8.0, RoadStatus.OPEN, 5002L);
        addEdge(edges, nodeMap, "KIRTINAGAR", "RUDRAPRAYAG_JN", "NH-7 (Kirtinagar-Rudraprayag)", "NH", 28.0, 42.0, RoadStatus.OPEN, 5003L);
        addEdge(edges, nodeMap, "RUDRAPRAYAG_JN", "GAUCHAR", "NH-7 (Rudraprayag-Gauchar)", "NH", 24.0, 38.0, RoadStatus.OPEN, 5004L);
        addEdge(edges, nodeMap, "GAUCHAR", "KARNAPRAYAG", "NH-7 (Gauchar-Karnaprayag)", "NH", 10.0, 15.0, RoadStatus.OPEN, 5005L);
        addEdge(edges, nodeMap, "KARNAPRAYAG", "NANDAPRAYAG", "NH-7 (Karnaprayag-Nandaprayag)", "NH", 20.0, 30.0, RoadStatus.OPEN, 5006L);
        addEdge(edges, nodeMap, "NANDAPRAYAG", "CHAMOLI_JN", "NH-7 (Nandaprayag-Chamoli)", "NH", 11.0, 16.0, RoadStatus.OPEN, 5007L);
        addEdge(edges, nodeMap, "CHAMOLI_JN", "PIPALKOTI", "NH-7 (Badrinath Highway Chamoli-Pipalkoti)", "NH", 16.0, 26.0, RoadStatus.OPEN, 5008L);
        addEdge(edges, nodeMap, "PIPALKOTI", "HELANG", "NH-7 (Pipalkoti-Helang)", "NH", 15.0, 25.0, RoadStatus.OPEN, 5009L);
        addEdge(edges, nodeMap, "HELANG", "JOSHIMATH", "NH-7 (Helang-Joshimath)", "NH", 14.0, 24.0, RoadStatus.OPEN, 5010L);
        addEdge(edges, nodeMap, "JOSHIMATH", "GOVINDGHAT", "NH-7 (Joshimath-Govindghat)", "NH", 19.0, 32.0, RoadStatus.OPEN, 5011L);
        addEdge(edges, nodeMap, "GOVINDGHAT", "PANDUKESHWAR", "NH-7 (Govindghat-Pandukeshwar)", "NH", 5.0, 10.0, RoadStatus.OPEN, 5012L);
        addEdge(edges, nodeMap, "PANDUKESHWAR", "BADRINATH", "NH-7 (Pandukeshwar-Badrinath)", "NH", 21.0, 45.0, RoadStatus.OPEN, 5013L);

        // Gopeshwar Spur (Connects Chamoli NH-7 to District HQ Depot & Hospital)
        addEdge(edges, nodeMap, "CHAMOLI_JN", "GOPESHWAR", "Gopeshwar Road", "SH", 10.0, 18.0, RoadStatus.OPEN, 5020L);

        // NH-107 Mandakini Corridor Segments
        // Tilwara Bridge has flood waters - marked FLOODED
        addEdge(edges, nodeMap, "RUDRAPRAYAG_JN", "TILWARA", "NH-107 (Rudraprayag-Tilwara Bridge)", "NH", 9.0, 14.0, RoadStatus.FLOODED, 5030L);
        addEdge(edges, nodeMap, "TILWARA", "AGASTMUNI", "NH-107 (Tilwara-Agastmuni)", "NH", 10.0, 15.0, RoadStatus.OPEN, 5031L);
        // Alternate bypass avoiding Tilwara Bridge
        addEdge(edges, nodeMap, "RUDRAPRAYAG_JN", "AGASTMUNI", "Mandakini East Bank Bypass", "SH", 18.0, 26.0, RoadStatus.OPEN, 5037L);

        // Agastmuni to Kund has rockfall warnings - marked UNCERTAIN
        addEdge(edges, nodeMap, "AGASTMUNI", "KUND_JN", "NH-107 (Agastmuni-Kund)", "NH", 18.0, 28.0, RoadStatus.UNCERTAIN, 5032L);
        addEdge(edges, nodeMap, "KUND_JN", "GUPTKASHI", "NH-107 (Kund-Guptkashi)", "NH", 7.0, 12.0, RoadStatus.OPEN, 5033L);
        addEdge(edges, nodeMap, "GUPTKASHI", "PHATA", "NH-107 (Guptkashi-Phata)", "NH", 14.0, 24.0, RoadStatus.OPEN, 5034L);
        addEdge(edges, nodeMap, "PHATA", "SONPRAYAG", "NH-107 (Phata-Sonprayag)", "NH", 15.0, 26.0, RoadStatus.OPEN, 5035L);
        addEdge(edges, nodeMap, "SONPRAYAG", "GAURIKUND", "NH-107 (Sonprayag-Gaurikund)", "NH", 5.0, 12.0, RoadStatus.OPEN, 5036L);

        // Chopta Mountain Bypass Corridor (Connects Mandakini Valley to Alaknanda Valley via Gopeshwar!)
        // This is the famous alternate route between Rudraprayag/Kund and Chamoli/Joshimath!
        addEdge(edges, nodeMap, "KUND_JN", "UKHIMATH", "Kund-Ukhimath Link", "SH", 6.0, 12.0, RoadStatus.OPEN, 5040L);
        addEdge(edges, nodeMap, "UKHIMATH", "CHOPTA_PASS", "Chopta Road (Ukhimath-Chopta)", "SH", 29.0, 52.0, RoadStatus.OPEN, 5041L);
        addEdge(edges, nodeMap, "CHOPTA_PASS", "MANDAL", "Mandal Valley Pass", "SH", 24.0, 42.0, RoadStatus.OPEN, 5042L);
        addEdge(edges, nodeMap, "MANDAL", "GOPESHWAR", "Mandal-Gopeshwar Route", "SH", 16.0, 28.0, RoadStatus.OPEN, 5043L);

        // Southern Connector via Tharali & Karnaprayag (NH-109)
        addEdge(edges, nodeMap, "KARNAPRAYAG", "THARALI", "NH-109 (Karnaprayag-Tharali)", "NH", 44.0, 70.0, RoadStatus.OPEN, 5050L);

        roadEdgeRepository.saveAll(edges);
        log.info("Saved {} road edges representing realistic OSM road network for Uttarakhand pilot district.", edges.size());
    }

    private void addNode(Map<String, RoadNode> map, String key, double lat, double lon, String name, Long osmId) {
        RoadNode node = RoadNode.builder()
            .latitude(lat)
            .longitude(lon)
            .name(name)
            .osmNodeId(osmId)
            .build();
        map.put(key, node);
    }

    private void addEdge(
        List<RoadEdge> list,
        Map<String, RoadNode> map,
        String fromKey,
        String toKey,
        String roadName,
        String roadType,
        double distanceKm,
        double baseTimeMinutes,
        RoadStatus status,
        Long osmWayId
    ) {
        RoadNode fromNode = map.get(fromKey);
        RoadNode toNode = map.get(toKey);
        if (fromNode == null || toNode == null) {
            log.error("Missing node {} or {} for edge {}", fromKey, toKey, roadName);
            return;
        }

        RoadEdge edge = RoadEdge.builder()
            .fromNode(fromNode)
            .toNode(toNode)
            .roadName(roadName)
            .roadType(roadType)
            .distanceKm(distanceKm)
            .baseTravelTimeMinutes(baseTimeMinutes)
            .status(status)
            .bidirectional(true)
            .osmWayId(osmWayId)
            .build();
        list.add(edge);
    }
}
