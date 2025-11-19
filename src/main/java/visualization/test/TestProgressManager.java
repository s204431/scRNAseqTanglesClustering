package visualization.test;

import org.nd4j.common.primitives.AtomicDouble;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TestProgressManager {
    private int size;
    private int configs;

    private String[] titles;

    public interface Listener {
        void onTangleFinished(int configIndex, int testIndex, double time, double nmi, double randIndex);
        void onPythonFinished(int testIndex, double time, double nmi, double randIndex);
        default void onRunFinished() {}
        default void onAllFinished() {}
    }
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private AtomicReferenceArray<AtomicBoolean[]> tangleFinished;
    private AtomicReferenceArray<AtomicDouble[]> tangleTimes;
    private AtomicReferenceArray<AtomicDouble[]> tangleNMI;
    private AtomicReferenceArray<AtomicDouble[]> tangleRandIndex;
    private AtomicBoolean[] pythonFinished;
    private AtomicDouble[] pythonTimes;
    private AtomicDouble[] pythonNMI;
    private AtomicDouble[] pythonRandIndex;

    private AtomicBoolean stopTesting = new AtomicBoolean(false);
    private AtomicInteger perTestUnits;
    private AtomicInteger totalUnits;
    private AtomicInteger finishedUnits;
    private AtomicInteger progress;     // The percentage of finished test units

    public TestProgressManager() {
        reset(0, 0);
    }

    public void markSingleRunFinished() {
        finishedUnits.incrementAndGet();
        fireRunFinished();
    }

    public void markTangleFinished(int configIndex, int i, double time, double nmi, double randIndex) {
        tangleFinished.get(configIndex)[i].set(true);
        tangleTimes.get(configIndex)[i].set(time);
        tangleNMI.get(configIndex)[i].set(nmi);
        tangleRandIndex.get(configIndex)[i].set(randIndex);

        fireTangleFinished(configIndex, i, time, nmi, randIndex);
    }

    public void markPythonFinished(int i, double time, double nmi, double randIndex) {
        pythonFinished[i].set(true);
        pythonTimes[i].set(time);
        pythonNMI[i].set(nmi);
        pythonRandIndex[i].set(randIndex);

        firePythonFinished(i, time, nmi, randIndex);
    }

    public void initializeProgress(int tests, int configs, int runs, boolean runPython) {
        int pythonUnits = tests * runs;
        int perTestUnits = configs * runs;
        int totalUnits = tests * perTestUnits + (runPython ? pythonUnits : 0);

        this.perTestUnits = new AtomicInteger(perTestUnits);
        this.totalUnits = new AtomicInteger(totalUnits);
        this.finishedUnits = new AtomicInteger(0);
    }

    public int getProgress() {
        return (int) (((double) finishedUnits.get() / totalUnits.get()) * 1000);
    }

    public boolean testingStopped() {
        return stopTesting.get();
    }

    public void setStopTesting(boolean stop) {
        stopTesting.set(stop);
    }

    public void setTitles(String[] titles) {
        this.titles = titles;
    }

    public boolean getTangleStatus(int configIndex, int i) {
        return tangleFinished.get(configIndex)[i].get();
    }

    public double getTangleTime(int configIndex, int i) {
        return tangleTimes.get(configIndex)[i].get();
    }

    public double getTangleNMI(int configIndex, int i) {
        return tangleNMI.get(configIndex)[i].get();
    }

    public double getTangleRandIndex(int configIndex, int i) {
        return tangleRandIndex.get(configIndex)[i].get();
    }

    public boolean getPythonStatus(int i) {
        return pythonFinished[i].get();
    }

    public double getPythonTime(int i) {
        return pythonTimes[i].get();
    }

    public double getPythonNMI(int i) {
        return pythonNMI[i].get();
    }

    public double getPythonRandIdx(int i) {
        return pythonRandIndex[i].get();
    }

    public int getSize() {
        return size;
    }

    public int getConfigsSize() {
        return configs;
    }

    public String getTitle(int i) {
        if (titles == null) return "";
        return titles[i];
    }

    public void reset(int size, int configurations) {
        this.size = size;
        this.configs = configurations;

        stopTesting = new AtomicBoolean(false);

        tangleFinished = new AtomicReferenceArray<>(configurations);
        tangleTimes = new AtomicReferenceArray<>(configurations);
        tangleNMI = new AtomicReferenceArray<>(configurations);
        tangleRandIndex = new AtomicReferenceArray<>(configurations);
        for (int i = 0; i < configurations; i++) {
            tangleFinished.set(i, new AtomicBoolean[size]);
            tangleTimes.set(i, new AtomicDouble[size]);
            tangleNMI.set(i, new AtomicDouble[size]);
            tangleRandIndex.set(i, new AtomicDouble[size]);
            for (int j = 0; j < size; j++) {
                tangleFinished.get(i)[j] = new AtomicBoolean(false);
                tangleTimes.get(i)[j] = new AtomicDouble();
                tangleNMI.get(i)[j] = new AtomicDouble();
                tangleRandIndex.get(i)[j] = new AtomicDouble();
            }
        }

        pythonFinished = new AtomicBoolean[size];
        pythonTimes = new AtomicDouble[size];
        pythonNMI = new AtomicDouble[size];
        pythonRandIndex = new AtomicDouble[size];
        for (int i = 0; i < size; i++) {
            pythonFinished[i] = new AtomicBoolean(false);
            pythonTimes[i] = new AtomicDouble();
            pythonNMI[i] = new AtomicDouble();
            pythonRandIndex[i] = new AtomicDouble();
        }
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void fireRunFinished() {
        for (Listener l : listeners) l.onRunFinished();
    }

    private void fireTangleFinished(int configIndex, int testIndex, double time, double nmi, double randIndex) {
        for (Listener l : listeners) l.onTangleFinished(configIndex, testIndex, time, nmi, randIndex);
    }

    private void firePythonFinished(int testIndex, double time, double nmi, double randIndex) {
        for (Listener l : listeners) l.onPythonFinished(testIndex, time, nmi, randIndex);
    }

    public void fireAllFinished() {
        for (Listener l : listeners) l.onAllFinished();
    }

    public int getTitleCount() {
        return titles.length;
    }
}
