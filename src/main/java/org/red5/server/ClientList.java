/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server;

import java.beans.ConstructorProperties;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client list, implemented using weak references to prevent memory leaks.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 * @param <E>
 *            type of class
 */
public class ClientList<E> extends AbstractList<E> {

    private CopyOnWriteArrayList<WeakReference<E>> items = new CopyOnWriteArrayList<WeakReference<E>>();

    @ConstructorProperties(value = { "" })
    public ClientList() {
    }

    @ConstructorProperties({ "c" })
    public ClientList(Collection<E> c) {
        addAll(0, c);
    }

    public boolean add(E element) {
        return items.add(new WeakReference<E>(element));
    }

    public void add(int index, E element) {
        items.add(index, new WeakReference<E>(element));
    }

    @Override
    public E remove(int index) {
        WeakReference<E> ref = items.remove(index);
        return ref.get();
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = false;
        E element = null;
        for (WeakReference<E> ref : items) {
            element = ref.get();
            if (element != null && element.equals(o)) {
                ref.clear();
                removed = true;
                break;
            }
        }
        return removed;
    }

    @Override
    public boolean contains(Object o) {
        List<E> list = new ArrayList<E>();
        for (WeakReference<E> ref : items) {
            if (ref.get() != null) {
                list.add(ref.get());
            }
        }
        boolean contains = list.contains(o);
        list.clear();
        list = null;
        return contains;
    }

    public int size() {
        removeReleased();
        return items.size();
    }

    public E get(int index) {
        return (items.get(index)).get();
    }

    private void removeReleased() {
        for (WeakReference<E> ref : items) {
            if (ref.get() == null) {
                items.remove(ref);
            }
        }
    }

}
