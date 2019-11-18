package petrinet;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

public class PetriNet<T> {
    private HashMap<T, Integer> places;
    private boolean fair;

    private List<Pair<Semaphore, Collection<Transition<T>>>> waitingThreads = new CopyOnWriteArrayList<>();
    private Semaphore fireProtection = new Semaphore(1, true);
    private Semaphore reachable = new Semaphore(1);


    public HashMap<T, Integer> getPlaces() {
        return new HashMap<>(places);
    }

    private boolean isFair() {
        return fair;
    }

    public PetriNet(Map<T, Integer> initial, boolean fair) {
        places = new HashMap<>();
        for (Map.Entry<T, Integer> e : initial.entrySet()) {
            places.put(e.getKey(), e.getValue());
        }
        this.fair = fair;
    }

    private PetriNet(PetriNet<T> pnet) {
        this.fair = pnet.isFair();
        places = new HashMap<>();
        for (Map.Entry<T, Integer> e : pnet.getPlaces().entrySet()) {
            places.put(e.getKey(), e.getValue());
        }
    }

    private boolean isEnabled(Transition<T> transition) {
        for (InputArc<T> arc : transition.getInputArcs()) {
            if (places.get(arc.getPlace()) == null || arc.getWeight() > places.get(arc.getPlace())) {
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

    private Transition<T> hasEnabled(Collection<Transition<T>> transitions) {
        Transition<T> t = null;

        for (Transition<T> e : transitions) {
            if (isEnabled(e)) {
                t = e;
                break;
            }
        }
        return t;
    }

    private void fireEnabled(Transition<T> t) {
        for (InputArc<T> arc : t.getInputArcs()) {
            places.replace(arc.getPlace(), places.get(arc.getPlace()) - arc.getWeight());
        }

        for (OutputArc<T> arc : t.getOutputArcs()) {
            places.replace(arc.getPlace(), places.get(arc.getPlace()) + arc.getWeight());
        }

        for (ResetArc<T> arc : t.getResetArcs()) {
            places.replace(arc.getPlace(), 0);
        }
    }

    private boolean setContains(Set<Map<T, Integer>> set, Map<T, Integer> map) {
        Map<T, Integer> m = new HashMap<>(map);

        //removes zero-token places from map
        Collection<T> toRemove = new ArrayList<>();
        for (T i : m.keySet()) {
            if (m.get(i) == 0)
                toRemove.add(i);
        }
        for (T i : toRemove) {
            m.remove(i);
        }

        for (Map<T, Integer> ma : set) {
            if (ma.equals(m))
                return true;
        }
        return false;
    }

    private void removePlacesWithNoTokens(Set<Map<T, Integer>> set) {
        for (Map<T, Integer> m : set) {
            Collection<T> toRemove = new ArrayList<>();
            for (T i : m.keySet()) {
                if (m.get(i) == 0) {
                    toRemove.add(i);
                }
            }
            for (T i : toRemove) {
                m.remove(i);
            }
        }
    }

    private void reachableRecursive(Set<Map<T, Integer>> reached, Collection<Transition<T>> transitions) {
        if (setContains(reached, getPlaces())) {
            return;
        }
        removePlacesWithNoTokens(reached);
        reached.add(getPlaces());

        for (Transition<T> t : transitions) {
            if (isEnabled(t)) {
                PetriNet<T> copy = new PetriNet<>(this);
                copy.addEmptyPlaces(t);
                copy.fireEnabled(t);
                copy.reachableRecursive(reached, transitions);
            }
        }
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
        Set<Map<T, Integer>> set = new HashSet<>();
        try {
            reachable.acquire();
            reachableRecursive(set, transitions);
            reachable.release();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        removePlacesWithNoTokens(set);
        return new HashSet<>(set);
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
        fireProtection.acquire();
        Transition<T> t = hasEnabled(transitions);
        if (t == null) {
            Pair<Semaphore, Collection<Transition<T>>> p = new Pair<>(new Semaphore(0), transitions);
            try {
                waitingThreads.add(p);
                fireProtection.release();
                p.first().acquire();
            } catch (InterruptedException e) {
                waitingThreads.remove(p);
                throw new InterruptedException();
            }
            t = hasEnabled(transitions);
        }

        addEmptyPlaces(t);
        fireEnabled(t);

        boolean released = false;
        if (waitingThreads.isEmpty()) {
            fireProtection.release();
        } else {
            for (Pair<Semaphore, Collection<Transition<T>>> p : waitingThreads) {
                if (hasEnabled(p.second()) != null) {
                    Semaphore s = p.first();
                    waitingThreads.remove(p);
                    s.release();
                    released = true;

                    break;
                }
            }
            if (!released) {
                fireProtection.release();
            }
        }
        return t;
    }
}
