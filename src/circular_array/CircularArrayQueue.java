package circular_array;

import java.util.*;

public class CircularArrayQueue<E> implements Queue<E> {
    private static final int DEFAULT_CAPACITY = 16;
    Object[] values;
    int capacity;
    int size = 0;
    int head = 0;
    int tail = -1;

    CircularArrayQueue() {
        this(DEFAULT_CAPACITY);
    }

    public CircularArrayQueue(int capacity) {
        this.capacity = capacity;
        this.values = new Object[this.capacity];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public void clear() {
        this.head = 0;
        this.tail = -1;
        this.size = 0;
    }

    private boolean addLastElement(E e) {
        this.tail = (this.tail + 1) % this.capacity;
        this.values[tail] = e;
        this.size++;
        return true;
    }

    @Override
    public boolean add(E e) {
        if (this.size == this.capacity) {
            throw new IllegalStateException("queue is full");
        }

        return addLastElement(e);
    }

    @Override
    public boolean offer(E e) {
        if (this.size == this.capacity) {
            return false;
        }

        return addLastElement(e);
    }

    private E firstElement() {
        E element = (E) this.values[this.head];
        this.head = (this.head + 1) % this.capacity;
        this.size--;
        return element;
    }

    @Override
    public E remove() {
        if (isEmpty()) {
            throw new NullPointerException("Queue is empty");
        }

        return firstElement();
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }

        return firstElement();
    }

    @Override
    public E element() {
        if (isEmpty()) {
            throw new NullPointerException("Queue is empty");
        }
        return (E) this.values[this.head];
    }

    @Override
    public E peek() {
        return isEmpty() ? null : (E) this.values[this.head];
    }

    @Override
    public boolean contains(Object o) {
        for (int i = this.head; i != this.tail; i = (i + 1) % this.capacity) {
            Object e = this.values[i];
            if (Objects.equals(e, o)) {
                return true;
            }
        }
        return Objects.equals(this.values[this.tail], o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object element : c) {
            if (!this.contains(element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (Object element : c) {
            this.add((E) element);
        }
        return true;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int index = head;

            public boolean hasNext() {
                return index != tail + 1;
            }

            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("no next element");
                }

                E element = (E) values[index];
                index = (index + 1) % capacity;
                return element;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[this.size()];

        int j = 0;
        for (int i = this.head; i != this.tail; i = (i + 1) % this.capacity)
            arr[j++] = this.values[i];
        arr[j] = this.values[this.tail];

        return arr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < this.size()) {
            a = Arrays.copyOf(a, this.size());
        }

        int j = 0;
        for (int i = this.head; i != this.tail; i = (i + 1) % this.capacity)
            a[j++] = (T) this.values[i];
        a[j] = (T) this.values[this.tail];

        return a;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (int i = this.head; i != this.tail; i = (i + 1) % this.capacity)
            sb.append(this.values[i]).append(" ");
        sb.append(this.values[this.tail]);
        sb.append("]");

        return sb.toString();
    }

    public static void main(String[] args) {
        CircularArrayQueue<Integer> q = new CircularArrayQueue<>(6);
        ArrayList<Integer> a = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        q.addAll(a);
        System.out.println(q);
        q.remove(); q.remove(); q.remove(); q.remove();
        q.add(7);
        q.add(8);

        for (Integer i : q) {
            System.out.println(i);
        }
    }
}
