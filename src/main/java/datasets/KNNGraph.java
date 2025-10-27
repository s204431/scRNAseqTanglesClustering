package datasets;

import org.datavec.api.writable.Text;
import smile.neighbor.KDTree;
import smile.neighbor.Neighbor;
import smile.math.distance.EuclideanDistance;

import java.util.*;

import util.BitSet;
import util.Distance;

public class KNNGraph {

    public List<List<Integer>> graph = new ArrayList<>(); //Directed graph
    public List<List<Integer>> graphUndirected = new ArrayList<>(); //Undirected graph
    public List<List<Double>> distances = new ArrayList<>(); //Directed distances
    public List<List<Double>> distancesUndirected = new ArrayList<>(); //Undirected distances
    public List<List<Integer>> rank = new ArrayList<>();

    public KNNGraph(double[][] data, int k) {
        KDTree<double[]> kdTree = new KDTree<>(data, data);
        for (int i = 0; i < data.length; i++) {
            graph.add(new ArrayList<>());
            graphUndirected.add(new ArrayList<>());
            distances.add(new ArrayList<>());
            distancesUndirected.add(new ArrayList<>());
            rank.add(new ArrayList<>());
        }
        for (int i = 0; i < data.length; i++) {
            Neighbor[] neighbors = kdTree.search(data[i], k);
            for (Neighbor neighbor : neighbors) {
                if (i != neighbor.index()) {
                    graph.get(i).add(neighbor.index());
                    distances.get(i).add(neighbor.distance());
                    //graphUndirected.get(i).add(neighbor.index());
                    //graphUndirected.get(neighbor.index()).add(i);
                    //distancesUndirected.get(i).add(neighbor.distance());
                    //distancesUndirected.get(neighbor.index()).add(neighbor.distance());
                }
            }
        }
        for (int i = 0; i < data.length; i++) {
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < graph.get(i).size(); j++) {
                indices.add(j);
            }
            List<Double> distance = distances.get(i);
            indices.sort(Comparator.comparingDouble(distance::get));
            List<Integer> newGraph = new ArrayList<>();
            List<Double> newDistances = new ArrayList<>();
            for (int index : indices) {
                newGraph.add(graph.get(i).get(index));
                newDistances.add(distances.get(i).get(index));
            }
            graph.set(i, newGraph);
            distances.set(i, newDistances);
            for (int j = 0; j < graph.get(i).size(); j++) {
                int index = graph.get(i).get(j);
                graphUndirected.get(index).add(i);
                distancesUndirected.get(index).add(distances.get(i).get(j));
                rank.get(index).add(indices.get(j));
            }
        }
    }

    //Returns the k nearest neighbours. This k must be no larger than the k used to generate the graph.
    public List[] getNeighbours(int index, int k, boolean directed) {
        List<Integer> neighbours = new ArrayList<>();
        List<Double> neighbourDistances = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            neighbours.add(graph.get(index).get(i));
            neighbourDistances.add(distances.get(index).get(i));
        }
        if (!directed) {
            for (int i = 0; i < graphUndirected.get(index).size(); i++) {
                if (rank.get(index).get(i) < k) {
                    neighbours.add(graphUndirected.get(index).get(i));
                    neighbourDistances.add(distancesUndirected.get(index).get(i));
                }
            }
        }
        return new List[] {neighbours, neighbourDistances};
    }

    //Returns a list of distances for edges with one end on each side of the cut.
    public List<Double> getDistancesBetween(BitSet cut) {
        List<Double> distancesBetween = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            for (int j = 0; j < graph.get(i).size(); j++) {
                if (cut.get(i) != cut.get(graph.get(i).get(j))) {
                    distancesBetween.add(distances.get(i).get(j));
                }
            }
        }
        return distancesBetween;
    }

    public List<List<Integer>> getConnectedComponents() {
        boolean[] visited = new boolean[graph.size()];
        List<List<Integer>> connectedComponents = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            if (visited[i]) {
                continue;
            }
            connectedComponents.add(new ArrayList<>());
            Queue<Integer> queue = new ArrayDeque<>();
            queue.add(i);
            visited[i] = true;
            connectedComponents.getLast().add(i);
            while (!queue.isEmpty()) {
                int node = queue.poll();
                List<Integer> neighbors = graphUndirected.get(node);
                for (int n : neighbors) {
                    if (!visited[n]) {
                        queue.add(n);
                        visited[n] = true;
                        connectedComponents.getLast().add(n);
                    }
                }
            }
        }
        return connectedComponents;
    }

    public void print() {
        for (int i = 0; i < graph.size(); i++) {
            System.out.println((i + " -> " + graph.get(i)).replace("[", "").replace("]", ""));
        }
    }

}
