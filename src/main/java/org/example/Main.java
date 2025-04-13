package org.example;

import java.util.concurrent.*;
import java.util.*;

class Magazyn {
    private Map<String, Integer> towary = new ConcurrentHashMap<>();
    private final int CALKOWITA_ILOSC = 50;
    private int obecnaIlosc = 0;

    public synchronized void produkuj(String typ, int ilosc) throws InterruptedException {
        while (obecnaIlosc + ilosc > CALKOWITA_ILOSC) {
            wait();
        }
        towary.put(typ, towary.getOrDefault(typ, 0) + ilosc);
        obecnaIlosc += ilosc;
        System.out.println("Produkowany towar " + typ + ", obecna ilość: " + towary.get(typ));
        wyswietlStanMagazynu();
        notifyAll();
    }

    public synchronized void konsumuj(String typ, int ilosc) throws InterruptedException {
        while (towary.getOrDefault(typ, 0) < ilosc) {
            ilosc = towary.getOrDefault(typ, 0);
        }
        towary.put(typ, towary.get(typ) - ilosc);
        obecnaIlosc -= ilosc;
        System.out.println("Konsumowany towar " + typ + ", obecna ilość: " + towary.get(typ));
        wyswietlStanMagazynu();
        notifyAll();
    }

    public boolean czyIstnieje(String typ) {
        return towary.containsKey(typ) && towary.get(typ) > 0;
    }

    public boolean czyMoznaDodac(String typ, int ilosc) {
        return obecnaIlosc + ilosc <= CALKOWITA_ILOSC;
    }

    public void wyswietlStanMagazynu() {
        System.out.println("Stan magazynu: " + towary);
    }

    public int getCalkowitaIlosc() {
        return CALKOWITA_ILOSC;
    }
}

class Producent implements Runnable {
    private Magazyn magazyn;
    private String[] typyTowarow;
    private volatile boolean isRunning = true;

    public Producent(Magazyn magazyn, String[] typyTowarow) {
        this.magazyn = magazyn;
        this.typyTowarow = typyTowarow;
    }

    public void run() {
        try {
            while (isRunning) {
                String typ = typyTowarow[ThreadLocalRandom.current().nextInt(typyTowarow.length)];
                int ilosc;
                do {
                    ilosc = ThreadLocalRandom.current().nextInt(1, magazyn.getCalkowitaIlosc()/2);
                } while (!magazyn.czyMoznaDodac(typ, ilosc));
                magazyn.produkuj(typ, ilosc);
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 5000));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stopRunning() {
        isRunning = false;
    }
}

class Konsument implements Runnable {
    private Magazyn magazyn;
    private String[] typyTowarow;
    private volatile boolean isRunning = true;

    public Konsument(Magazyn magazyn, String[] typyTowarow) {
        this.magazyn = magazyn;
        this.typyTowarow = typyTowarow;
    }

    public void run() {
        try {
            while (isRunning) {
                String typ = typyTowarow[ThreadLocalRandom.current().nextInt(typyTowarow.length)];
                int ilosc = ThreadLocalRandom.current().nextInt(1, magazyn.getCalkowitaIlosc());
                if (magazyn.czyIstnieje(typ)) {
                    magazyn.konsumuj(typ, ilosc);
                } else {
                    System.out.println("Towar " + typ + " nie jest dostępny w magazynie.");
                }
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 5000));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stopRunning() {
        isRunning = false;
    }
}

public class Main {
    public static void main(String[] args) {
        Magazyn magazyn = new Magazyn();
        String[] typyTowarow = {"towar1", "towar2", "towar3"};
        int liczbaProducentow = 5;
        int liczbaKonsumentow = 5;

        ExecutorService producenci = Executors.newFixedThreadPool(liczbaProducentow);
        ExecutorService konsumenci = Executors.newFixedThreadPool(liczbaKonsumentow);

        List<Producent> listaProducentow = new ArrayList<>();
        for (int i = 0; i < liczbaProducentow; i++) {
            Producent producent = new Producent(magazyn, typyTowarow);
            listaProducentow.add(producent);
            producenci.execute(producent);
        }

        List<Konsument> listaKonsumentow = new ArrayList<>();
        for (int i = 0; i < liczbaKonsumentow; i++) {
            Konsument konsument = new Konsument(magazyn, typyTowarow);
            listaKonsumentow.add(konsument);
            konsumenci.execute(konsument);
        }

        Thread userInputThread = new Thread(() -> {
            try {
                System.in.read();
                for (Producent producent : listaProducentow) {
                    producent.stopRunning();
                }
                for (Konsument konsument : listaKonsumentow) {
                    konsument.stopRunning();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        userInputThread.start();

        try {
            userInputThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        producenci.shutdownNow();
        konsumenci.shutdownNow();
    }
}
