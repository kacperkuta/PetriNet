package petrinet;

import java.util.*;
import java.util.concurrent.Semaphore;

public class PetriNet<T> {
    private HashMap<T, Integer> places;
    private boolean fair;

    private Semaphore waitingThreads;
    private Semaphore fireProtection = new Semaphore(1, true);

    private int countUnfireable = 0;


    public HashMap<T, Integer> getPlaces() {
        return new HashMap<>(places);
    }

    public boolean isFair() {
        return fair;
    }

    public PetriNet(Map<T, Integer> initial, boolean fair) {
        places = new HashMap<>();
        for (Map.Entry<T, Integer> e : initial.entrySet()) {
            places.put(e.getKey(), e.getValue());
        }
        this.fair = fair;
        waitingThreads = new Semaphore(0, fair);

    }

    public PetriNet(PetriNet<T> pnet) {
        this.fair = pnet.isFair();
        places = new HashMap<>();
        for (Map.Entry<T, Integer> e : pnet.getPlaces().entrySet()) {
            places.put(e.getKey(), e.getValue());
        }
    }

    private boolean isReachable(Transition<T> transition) {
        for (InputArc<T> arc : transition.getInputArcs()) {
            if (places.get(arc.getPlace()) != null && arc.getWeight() > places.get(arc.getPlace())) {
                return false;
            }
        }
        for (InhibitorArc<T> arc : transition.getInhibitorArcs()) {
            if (places.get(arc.getPlace()) != null && places.get(arc.getPlace()) != 0) {
                return false;
            }
        }
        return true;
    }

    private void addEmptyPlaces(Transition<T> transition) {
        for (InputArc<T> arc : transition.getInputArcs()) {
            places.putIfAbsent(arc.getPlace(), 0);
        }
        for (InhibitorArc<T> arc : transition.getInhibitorArcs()) {
            places.putIfAbsent(arc.getPlace(), 0);
        }
        for (ResetArc<T> arc : transition.getResetArcs()) {
            places.putIfAbsent(arc.getPlace(), 0);
        }
        for (OutputArc<T> arc : transition.getOutputArcs()) {
            places.putIfAbsent(arc.getPlace(), 0);
        }
    }

    private Transition<T> hasReachable(Collection<Transition<T>> transitions) {
        Transition<T> t = null;

        for (Transition<T> e : transitions) {
            if (isReachable(e)) {
                t = e;
                break;
            }
        }
        return t;
    }

    private void fireReachable(Transition<T> t) {
        for (InputArc<T> arc : t.getInputArcs()) {
            if (places.get(arc.getPlace()) != null)
                places.put(arc.getPlace(), places.get(arc.getPlace()) - arc.getWeight());
        }

        for (OutputArc<T> arc : t.getOutputArcs()) {
            if (places.get(arc.getPlace()) != null)
                places.put(arc.getPlace(), places.get(arc.getPlace()) + arc.getWeight());
        }

        for (ResetArc<T> arc : t.getResetArcs()) {
            if (places.get(arc.getPlace()) != null)
                places.put(arc.getPlace(), 0);
        }
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
        Transition<T> t = hasReachable(transitions);

        if (t == null) {
            waitingThreads.acquire();
            while (true) {
                if (waitingThreads.getQueueLength() > countUnfireable) {
                    t = hasReachable(transitions);
                    if (t != null) {
                        fireReachable(t);
                        countUnfireable = 0;
                        waitingThreads.release();
                        return t;
                    } else {
                        waitingThreads.release();
                        waitingThreads.acquire();
                    }
                } else {
                    fireProtection.release();
                    waitingThreads.acquire();
                }
            }
        } else {
            fireProtection.acquire();
            addEmptyPlaces(t);
            fireReachable(t);
            countUnfireable = 0;
            waitingThreads.release();
            return t;
        }
    }

    private boolean setContains(Set<Map<T, Integer>> set, Map<T, Integer> map) {
        for (Map<T, Integer> m : set) {
            if (m.equals(map))
                return true;
        }
        return false;
    }

    private void removePlacesWithNoTokens(Set<Map<T, Integer>> set) {
        for (Map<T, Integer> m : set) {
            Collection<T> toRemove = new ArrayList<T>();
            for (T i : m.keySet()) {
                if (m.get(i) == 0)
                    toRemove.add(i);
            }
            for (T i : toRemove) {
                m.remove(i);
            }
        }
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
        //System.out.println(this.toString());

        Set<Map<T, Integer>> set = new HashSet<>();
        for (Transition<T> t : transitions) {
            if (isReachable(t)) {
                PetriNet<T> copy = new PetriNet<>(this);
                copy.addEmptyPlaces(t);
                copy.fireReachable(t);
                Set<Map<T, Integer>> result = copy.reachable(transitions);
                for (Map<T, Integer> m : result) {
                    if (!setContains(set, m))
                        set.add(m);
                }
            }
        }
        if (!setContains(set, getPlaces()))
            set.add(getPlaces());

        removePlacesWithNoTokens(set);
        //set = new HashSet<>(set);
        return set;
    }

    public String toString() {
        String places = "Places: \n";
        for (Map.Entry<T, Integer> e : getPlaces().entrySet()) {
            places += e.getKey();
            places += " : ";
            places += e.getValue();
            places += ", ";
        }
        return places;
    }
}
