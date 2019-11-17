package multiplicator;

import petrinet.PetriNet;
import petrinet.Transition;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class Main {

    private static final Integer THREADS = 4;

    private static final String A = "A";
    private static final String B = "B";
    private static final String result = "RESULT";
    private static final String resultDivided = "RESULTDIVIDED";

    private static Transition<String> createTransition(String in, int outputValue) {
        Map<String, Integer> input = new HashMap<>();
        input.put(in, 1);

        Map<String, Integer> output = new HashMap<>();
        output.put(result, outputValue);

        return new Transition<>(input, Collections.emptySet(), Collections.emptySet(), output);
    }

    private static PetriNet<String> createNet(int a, int b) {
        Map<String, Integer> init = new HashMap<>();
        init.put(A, a);
        init.put(B, b);
        return new PetriNet<>(init, true);
    }

    private static Collection<Transition<String>> createTransitions(int a, int b) {
        Transition<String> divide = new Transition<>(Collections.singletonMap(result, 2),
                Collections.emptySet(), Collections.emptySet(), Collections.singletonMap(resultDivided, 1));
        Transition<String> ta = createTransition(A, b);
        Transition<String> tb = createTransition(B, a);

        Collection<Transition<String>> collection = new HashSet<>();
        collection.add(ta);
        collection.add(tb);
        collection.add(divide);
        return collection;
    }

    private static class Count implements Runnable {

        private PetriNet<String> net;
        private Collection<Transition<String>> transitions;

        @Override
        public void run() {
            int i = 0;
            while (true) {
                try {
                    net.fire(transitions);
                    i++;
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + " fired: " + i);
                }
            }
        }

        public Count(PetriNet<String> net, Collection<Transition<String>> transitions) {
            this.net = net;
            this.transitions = transitions;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner scan = new Scanner(System.in);
        int a = scan.nextInt();
        int b = scan.nextInt();

        PetriNet<String> net = createNet(a, b);

        Collection<Transition<String>> collection = createTransitions(a, b);

        Set<String> set = new HashSet<>();
        set.add(A);
        set.add(B);
        set.add(result);

        Transition<String> finish = new Transition<>(Collections.emptyMap(), Collections.emptySet(), set, Collections.emptyMap());

        Set<Thread> threads = new HashSet<>();
        for (int i = 0 ; i < THREADS; i++) {
            Thread t = new Thread(new Count(net, collection));
            threads.add(t);
            t.start();
        }

        Thread t = new Thread(() -> {
            try {
                net.fire(Collections.singleton(finish));
                System.out.println("Result: "+ net.getPlaces().get(resultDivided));
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " interupted");
            }
        });
        t.start();
        t.join();

        for (Thread th : threads) {
            th.interrupt();
        }

    }
}
