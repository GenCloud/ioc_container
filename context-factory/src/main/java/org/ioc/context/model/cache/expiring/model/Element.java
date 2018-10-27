/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.context.model.cache.expiring.model;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class Element<K, V> {
	private final K key;
	private final V value;
	private long writeTime;
	private volatile long accessTime;
	private Element<K, V> before;
	private Element<K, V> after;
	private State state = State.NEW;

	public Element(K key, V value, long writeTime) {
		this.key = key;
		this.value = value;

		setAccessTime(writeTime);
		setWriteTime(writeTime);
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public long getWriteTime() {
		return writeTime;
	}

	public void setWriteTime(long writeTime) {
		this.writeTime = writeTime;
	}

	public long getAccessTime() {
		return accessTime;
	}

	public void setAccessTime(long accessTime) {
		this.accessTime = accessTime;
	}

	public Element<K, V> getBefore() {
		return before;
	}

	public void setBefore(Element<K, V> before) {
		this.before = before;
	}

	public Element<K, V> getAfter() {
		return after;
	}

	public void setAfter(Element<K, V> after) {
		this.after = after;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public enum State {
		NEW, EXISTING, DELETED
	}
}
