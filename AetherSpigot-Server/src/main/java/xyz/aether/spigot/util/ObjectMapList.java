package xyz.aether.spigot.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractReferenceList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectSpliterator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;

public final class ObjectMapList<T> extends AbstractReferenceList<T> implements Set<T> {
    private final Int2IntOpenHashMap objectToIndex;
    private static final Object[] EMPTY_LIST = new Object[0];
    @SuppressWarnings("unchecked")
    private T[] elements = (T[]) EMPTY_LIST;
    private int count;

    public ObjectMapList() {
        this(2, 0.8F);
    }

    public ObjectMapList(int expectedSize, float loadFactor) {
        this.objectToIndex = new Int2IntOpenHashMap(expectedSize, loadFactor);
        this.objectToIndex.defaultReturnValue(Integer.MIN_VALUE);
    }

    @Override
    public int size() {
        return this.count;
    }

    @Override
    public int indexOf(Object object) {
        return this.objectToIndex.get(object.hashCode());
    }

    @Override
    public int lastIndexOf(Object object) {
        return this.indexOf(object);
    }

    @Override
    public boolean remove(Object object) {
        int index = this.objectToIndex.remove(object.hashCode());
        if (index == Integer.MIN_VALUE) {
            return false;
        }

        int endIndex = --this.count;
        T end = this.elements[endIndex];
        if (index != endIndex) {
            this.objectToIndex.put(end.hashCode(), index);
        }
        this.elements[index] = end;
        this.elements[endIndex] = null;
        return true;
    }

    @Override
    public boolean add(T object) {
        int count = this.count;
        int currIndex = this.objectToIndex.putIfAbsent(object.hashCode(), count);
        if (currIndex != Integer.MIN_VALUE) {
            return false;
        }

        T[] arrayOfT = this.elements;
        if (arrayOfT.length == count) {
            arrayOfT = this.elements = Arrays.copyOf(arrayOfT, (int) Math.max(4L, (long) count << 1L));
        }

        arrayOfT[count] = object;
        this.count = count + 1;
        return true;
    }

    @Override
    public void add(int index, T object) {
        int currIndex = this.objectToIndex.putIfAbsent(object.hashCode(), index);
        if (currIndex != Integer.MIN_VALUE) {
            return;
        }

        int count = this.count;
        T[] arrayOfT = this.elements;
        if (arrayOfT.length == count) {
            arrayOfT = this.elements = Arrays.copyOf(arrayOfT, (int) Math.max(4L, (long) count << 1L));
        }

        System.arraycopy(arrayOfT, index, arrayOfT, index + 1, count - index);
        arrayOfT[index] = object;
        this.count = count + 1;
    }

    @Override
    public T get(int index) {
        return this.elements[index];
    }

    @Override
    public boolean isEmpty() {
        return (this.count == 0);
    }

    @Override
    public void clear() {
        this.objectToIndex.clear();
        Arrays.fill(this.elements, 0, this.count, null);
        this.count = 0;
    }

    @Override
    public Object @NotNull [] toArray() {
        return Arrays.copyOf(this.elements, this.count);
    }

    @Override
    public ObjectSpliterator<T> spliterator() {
        return super.spliterator();
    }

    @Override
    @NotNull
    public ObjectListIterator<T> iterator() {
        return new Iterator(0);
    }

    private class Iterator implements ObjectListIterator<T> {
        T lastRet;
        int current;

        Iterator(int index) {
            this.current = index;
        }

        @Override
        public int nextIndex() {
            return this.current + 1;
        }

        @Override
        public int previousIndex() {
            return this.current - 1;
        }

        @Override
        public boolean hasNext() {
            return (this.current < ObjectMapList.this.count);
        }

        @Override
        public boolean hasPrevious() {
            return (this.current > 0);
        }

        @Override
        public T next() {
            if (this.current >= ObjectMapList.this.count) {
                throw new NoSuchElementException();
            }
            return this.lastRet = ObjectMapList.this.elements[this.current++];
        }

        @Override
        public T previous() {
            if (this.current <= 0) {
                throw new NoSuchElementException();
            }
            return this.lastRet = ObjectMapList.this.elements[--this.current];
        }

        @Override
        public void remove() {
            T lastRet = this.lastRet;
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            this.lastRet = null;
            ObjectMapList.this.remove(lastRet);
            this.current--;
        }
    }
}
