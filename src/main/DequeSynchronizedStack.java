package main;

import java.util.ArrayDeque;
import java.util.Optional;

public class DequeSynchronizedStack<T> {

	// Internal Deque which gets decorated for synchronization.
	private ArrayDeque<T> dequeStore;

	public DequeSynchronizedStack(int initialCapacity) {
	        this.dequeStore = new ArrayDeque<>(initialCapacity);
	    }

	public DequeSynchronizedStack() {
	        dequeStore = new ArrayDeque<>();
	    }

	public synchronized Optional<T> pop() {
		if(this.size() > 0)
			return Optional.of(this.dequeStore.pop());
		return Optional.empty();
	}

	public synchronized void push(T element) {
		this.dequeStore.push(element);
	}

	public synchronized T peek() {
		return this.dequeStore.peek();
	}

	public synchronized int size() {
		return this.dequeStore.size();
	}
}