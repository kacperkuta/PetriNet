package alternator;

import petrinet.PetriNet;
import petrinet.Transition;

import java.util.*;

public class Main {
    private static final String process1 = "p1";
    private static final String process2 = "p2";
    private static final String process3 = "p3";
    private static final String box1 = "b1";
    private static final String box2 = "b2";
    private static final String box3 = "b3";
    private static final String SK = "sk";

    private static Transition<String> entryProtocol(String process, String box, String box_1, String box_2) {
        Map<String, Integer> inputArcs = new HashMap<>();
        inputArcs.put(process, 1);

        Map<String, Integer> outputArcs = new HashMap<>();
        outputArcs.put(SK, 1);

        Set<String> inhibitorArcs = new HashSet<>();
        inhibitorArcs.add(box);
        inhibitorArcs.add(SK);

        Set<String> resetArcs = new HashSet<>();
        resetArcs.add(box_1);
        resetArcs.add(box_2);

        return new Transition<>(inputArcs, resetArcs, inhibitorArcs, outputArcs);
    }

    private static Transition<String> finalProtocol(String process, String my_box, String box_a, String box_b) {
        Map<String, Integer> inputArcs = new HashMap<>();
        inputArcs.put(SK, 1);

        Collection<String> inhibitorArcs = Collections.singleton(process);

        Set<String> resetArcs = new HashSet<>();
        resetArcs.add(box_a);
        resetArcs.add(box_b);

        Map<String, Integer> outputArcs = new HashMap<>();
        outputArcs.put(my_box, 1);
        outputArcs.put(process, 1);


        return new Transition<>(inputArcs, resetArcs, inhibitorArcs, outputArcs);
    }

    private static PetriNet<String> createPetriNet() {
        Map<String, Integer> initial = new HashMap<>();
        initial.put(process1, 1);
        initial.put(process2, 1);
        initial.put(process3, 1);
        return new PetriNet<>(initial, false);
    }

    private static class Runner implements Runnable {

        Transition<String> fin;
        Transition<String> ent;
        PetriNet<String> net;

        public Runner(Transition<String> fin, Transition<String> ent, PetriNet<String> net) {
            this.fin = fin;
            this.ent = ent;
            this.net = net;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    net.fire(Collections.singleton(ent));
                    System.out.println(Thread.currentThread().getName());
                    //System.out.print(".");
                    net.fire(Collections.singleton(fin));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(Thread.currentThread().getName() + " interrupted.");
            }
        }
    }

    public static void main(String[] args) {
        PetriNet<String> net = createPetriNet();

        Transition<String> entry1 = entryProtocol(process1, box1, box2, box3);
        Transition<String> entry2 = entryProtocol(process2, box2, box1, box3);
        Transition<String> entry3 = entryProtocol(process3, box3, box1, box2);

        Transition<String> final1 = finalProtocol(process1, box1, box2, box3);
        Transition<String> final2 = finalProtocol(process2, box2, box1, box3);
        Transition<String> final3 = finalProtocol(process3, box3, box1, box2);

        Set<Transition<String>> set = new HashSet<>();
        set.add(entry1); set.add(entry2); set.add(entry3);
        set.add(final1); set.add(final2); set.add(final3);

        System.out.println(net.reachable(set).size());

        Thread t1 = new Thread(new Runner(final1, entry1, net));

        Thread t2 = new Thread(new Runner(final2, entry2, net));

        Thread t3 = new Thread(new Runner(final3, entry3, net));

        t1.start();
        t2.start();
        t3.start();

        try {
            Thread.sleep(30000);
            t1.interrupt();
            t2.interrupt();
            t3.interrupt();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

    }

}
