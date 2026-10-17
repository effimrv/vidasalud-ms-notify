package com.vidasalud.notify.actividad;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Guarda en memoria los últimos elementos procesados por un consumidor, para consultarlos por la API. */
public class RegistroActividad<T> {

    private static final int MAXIMO = 50;

    private final Deque<T> elementos = new ConcurrentLinkedDeque<>();

    public void agregar(T elemento) {
        elementos.addFirst(elemento);
        while (elementos.size() > MAXIMO) {
            elementos.pollLast();
        }
    }

    public List<T> recientes() {
        return List.copyOf(elementos);
    }
}
