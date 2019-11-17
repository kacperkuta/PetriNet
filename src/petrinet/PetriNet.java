package petrinet;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PetriNet<T> {
    private HashMap<T, Integer> places;
    private boolean fair;

    private Semaphore waitingThreads;
    private Semaphore fireProtection = new Semaphore(1, true);

    private AtomicInteger waiting = new AtomicInteger(0);
    private String firstName;


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

    private boolean isEnabled(Transition<T> transition) {
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
            //System.out.println("input : "+ arc.getPlace());
            places.replace(arc.getPlace(), places.get(arc.getPlace()) - arc.getWeight());
        }

        for (OutputArc<T> arc : t.getOutputArcs()) {
            //System.out.println("output : "+ arc.getPlace());
            places.replace(arc.getPlace(), places.get(arc.getPlace()) + arc.getWeight());
        }

        for (ResetArc<T> arc : t.getResetArcs()) {
            //System.out.println("reset : "+ arc.getPlace());
            places.replace(arc.getPlace(), 0);
        }
    }

    public void releaseHungered() throws InterruptedException {
        fireProtection.acquire();
        if (waitingThreads.hasQueuedThreads())
            waitingThreads.release();
        else
            fireProtection.release();
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
        fireProtection.acquire();
        Thread.sleep(15);
        //System.out.println(Thread.currentThread().getName() + " start");
        //System.out.println(waiting);
        while (true) {
            Transition<T> t = hasEnabled(transitions);
            if (t == null) {
                if (waiting.get() == 0) {
                    fireProtection.release();
                }
                else {
                    waitingThreads.release();
                }
                waiting.getAndIncrement();
                waitingThreads.acquire();
                waiting.getAndDecrement();
            } else {
                addEmptyPlaces(t);
                fireEnabled(t);
                //System.out.println(Thread.currentThread().getName() + " finish");
                if (waiting.get() == 0)
                    fireProtection.release();
                else
                    waitingThreads.release();
                return t;
            }
        }

        /*


        if (t == null) {
            if (waitingThreads.hasQueuedThreads()) {
                waitingThreads.release();
                System.out.println(waitingThreads.hasQueuedThreads());
            } else {
                fireProtection.release();
            }
            waitingThreads.acquire();
            while (true) {
                if (waitingThreads.getQueueLength() > countUnfireable) {
                    t = hasEnabled(transitions);
                    if (t != null) {
                        addEmptyPlaces(t);
                        fireEnabled(t);
                        countUnfireable = 0;
                        if (waitingThreads.hasQueuedThreads())
                            waitingThreads.release();
                        else
                            fireProtection.release();
                        System.out.println(Thread.currentThread().getName() + " finish");

                        return t;
                    } else {
                        if (waitingThreads.hasQueuedThreads())
                            waitingThreads.release();
                        else
                            fireProtection.release();
                        waitingThreads.acquire();
                    }
                } else {
                    fireProtection.release();
                    waitingThreads.acquire();
                }
            }
        } else {
            addEmptyPlaces(t);
            fireEnabled(t);
            countUnfireable = 0;
            if (waitingThreads.hasQueuedThreads())
                waitingThreads.release();
            else
                fireProtection.release();
            //System.out.print(toString());
            System.out.println(Thread.currentThread().getName() + " finish");

            return t;
        }
        */

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
        Set<Map<T, Integer>> set = new HashSet<>();
        for (Transition<T> t : transitions) {
            if (isEnabled(t)) {
                PetriNet<T> copy = new PetriNet<>(this);
                copy.addEmptyPlaces(t);
                copy.fireEnabled(t);
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
        set = new HashSet<>(set);
        return set;
    }

    @Override
    public String toString() {
        String places = "Places: \n";
        for (Map.Entry<T, Integer> e : getPlaces().entrySet()) {
            places += e.getKey();
            places += " : ";
            places += e.getValue();
            places += ", ";
        }
        places += "\n";
        return places;
    }
}
