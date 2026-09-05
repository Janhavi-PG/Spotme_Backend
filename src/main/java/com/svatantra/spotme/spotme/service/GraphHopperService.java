package com.svatantra.spotme.spotme.service;
import com.svatantra.common.logging.StructuredLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import java.util.Map;
import com.svatantra.common.exception.ExternalServiceException;
import org.springframework.http.HttpStatus;
import java.util.List;
import com.svatantra.spotme.watchtower.dto.RoutePointResponse;
import java.util.ArrayList;
import com.svatantra.spotme.spotme.dto.activities.ActivityItem;
import org.springframework.web.reactive.function.client.WebClient;
@Service
public class GraphHopperService {
    private final WebClient webClient;

    public GraphHopperService(
            @Qualifier("graphHopperWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }

    @Value("${graphhopper.api.key}")
    private String apiKey;

    private static final StructuredLogger log =
            StructuredLogger.forClass(GraphHopperService.class);
    public double calculateDistance(
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng
    ) {
        try {

            String url =
                    "https://graphhopper.com/api/1/route"
                            + "?point=" + originLat + "," + originLng
                            + "&point=" + destinationLat + "," + destinationLng
                            + "&profile=bike"
                            + "&points_encoded=false"
                            + "&key=" + apiKey;

            log.event("GRAPHHOPPER_REQUEST")
                    .field("originLat", originLat)
                    .field("originLng", originLng)
                    .field("destinationLat", destinationLat)
                    .field("destinationLng", destinationLng)
                    .info();
            System.out.println("GRAPHHOPPER URL = " + url);
            Map response =
                    webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            log.event("GRAPHHOPPER_RESPONSE_RECEIVED")
                    .info();
            if (response == null
                    || response.get("paths") == null) {

                throw new ExternalServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "GraphHopper",
                        "Unexpected response from GraphHopper"
                );
            }

            List<Map<String, Object>> paths =
                    (List<Map<String, Object>>) response.get("paths");
            if (paths.isEmpty()) {

                throw new ExternalServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "GraphHopper",
                        "Unexpected response from GraphHopper"
                );
            }
            Map<String, Object> firstPath =
                    paths.get(0);

            Double distance =
                    (Double) firstPath.get("distance");
            if (distance == null) {

                throw new ExternalServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "GraphHopper",
                        "Unexpected response from GraphHopper"
                );
            }
            log.event("GRAPHHOPPER_DISTANCE_CALCULATED")
                    .field("distanceMeters", distance)
                    .info();

            return distance;

        } catch (ExternalServiceException exception) {
            throw exception;
        }
        catch (ResourceAccessException exception) {

            log.event("GRAPHHOPPER_CALL_FAILED")
                    .error(exception);

            throw new ExternalServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "GraphHopper",
                    "Unable to establish a secure connection to GraphHopper",
                    exception
            );
        }
        catch (Exception exception) {

            log.event("GRAPHHOPPER_CALL_FAILED")
                    .error(exception);

            throw new ExternalServiceException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "GraphHopper",
                    "GraphHopper did not respond",
                    exception
            );
        }
    }

    public List<RoutePointResponse> getRoutePoints(
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng
    ) {
        try {

            String url =
                    "https://graphhopper.com/api/1/route"
                            + "?point=" + originLat + "," + originLng
                            + "&point=" + destinationLat + "," + destinationLng
                            + "&profile=car"
                            + "&points_encoded=false"
                            + "&key=" + apiKey;

            Map response =
                    webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            System.out.println("ROUTE RESPONSE = " + response);
            List<Map<String, Object>> paths =
                    (List<Map<String, Object>>) response.get("paths");

            if (paths == null || paths.isEmpty()) {

                throw new ExternalServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "GraphHopper",
                        "No route returned by GraphHopper"
                );
            }

            Map<String, Object> firstPath =
                    paths.get(0);
            Map<String, Object> points =
                    (Map<String, Object>) firstPath.get("points");

            if (points == null) {

                throw new ExternalServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "GraphHopper",
                        "Route points missing in GraphHopper response"
                );
            }

            List<List<Double>> coordinates =
                    (List<List<Double>>) points.get("coordinates");

            if (coordinates == null || coordinates.isEmpty()) {

                throw new ExternalServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "GraphHopper",
                        "Route coordinates missing in GraphHopper response"
                );
            }

            List<RoutePointResponse> routePoints =
                    new ArrayList<>();
            for (List<Double> coordinate : coordinates) {

                double lng = coordinate.get(0);
                double lat = coordinate.get(1);

                routePoints.add(
                        new RoutePointResponse(
                                lat,
                                lng
                        )
                );
            }
            System.out.println(
                    "ROUTE POINT COUNT = "
                            + routePoints.size()
            );

            return routePoints;

        } catch (Exception exception) {

            throw new ExternalServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "GraphHopper",
                    "Unable to fetch route points",
                    exception
            );
        }
    }
    public List<RoutePointResponse> getRoutePointsForDay(
            List<ActivityItem> activities
    ) {

        StringBuilder pointBuilder =
                new StringBuilder();
        int pointCount = 0;
        for (ActivityItem activity : activities) {
            if (pointCount >= 5) {
                break;
            }

            if (activity.originLat() == null
                    || activity.originLng() == null
                    || activity.destinationLat() == null
                    || activity.destinationLng() == null) {

                continue;
            }
            pointBuilder.append(

                    "&point="
                            + activity.originLat()
                            + ","
                            + activity.originLng()
            );
            pointCount++;
            if (pointCount >= 5) {
                break;
            }
            pointBuilder.append(

                    "&point="
                            + activity.destinationLat()
                            + ","
                            + activity.destinationLng()
            );
            pointCount++;
            if (pointCount >= 5) {
                break;
            }
        }

        String url =
                "https://graphhopper.com/api/1/route?"
                        + pointBuilder.substring(1)
                        + "&profile=car"
                        + "&points_encoded=false"
                        + "&key=" + apiKey;

        System.out.println(
                "DAY ROUTE URL = "
                        + url
        );

        Map response =
                webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
        List<Map<String, Object>> paths =
                (List<Map<String, Object>>) response.get("paths");

        System.out.println(
                "PATH COUNT = "
                        + paths.size()
        );
        Map<String, Object> firstPath =
                paths.get(0);

        System.out.println(
                "FIRST PATH FOUND"
        );
        Map<String, Object> points =
                (Map<String, Object>) firstPath.get("points");

        System.out.println(
                "POINTS FOUND = "
                        + (points != null)
        );
        List<List<Double>> coordinates =
                (List<List<Double>>) points.get("coordinates");

        System.out.println(
                "COORDINATE COUNT = "
                        + coordinates.size()
        );
        List<RoutePointResponse> routePoints =
                new ArrayList<>();
        for (List<Double> coordinate : coordinates) {

            double lng = coordinate.get(0);
            double lat = coordinate.get(1);

            routePoints.add(
                    new RoutePointResponse(
                            lat,
                            lng
                    )
            );
        }
        System.out.println(
                "ROUTE POINT COUNT = "
                        + routePoints.size()
        );
        return routePoints;

    }

}

