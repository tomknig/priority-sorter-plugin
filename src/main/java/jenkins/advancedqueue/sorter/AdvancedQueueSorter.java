/*
 * The MIT License
 *
 * Copyright (c) 2013, Magnus Sandberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.advancedqueue.sorter;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.Queue.Item;
import hudson.model.Queue.LeftItem;
import hudson.model.queue.QueueSorter;
import hudson.queueSorter.PrioritySorterQueueSorter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.advancedqueue.PrioritySorterConfiguration;

/**
 * @author Magnus Sandberg
 * @since 2.0
 */
@Extension
public class AdvancedQueueSorter extends QueueSorter {

	// Keeps track of what weighted-prio each buildableItems item has
	Map<Integer, Float> item2weight = new HashMap<Integer, Float>();

	public AdvancedQueueSorter() {
		super();
		List<BuildableItem> items = Queue.getInstance().getBuildableItems();
		// Sort the queue in the order the items entered the queue
		// so that onNewItem() happens in the correct order below
		Collections.sort(items, new Comparator<BuildableItem>() {
			public int compare(BuildableItem o1, BuildableItem o2) {
				return (int) (o1.getInQueueSince() - o2.getInQueueSince());
			}
		});
		for (BuildableItem item : items) {
			onNewItem(item);
		}
	}

	@Override
	public void sortBuildableItems(List<BuildableItem> items) {
		// Handle Legacy mode
		if (PrioritySorterConfiguration.get().getLegacyMode()) {
			new PrioritySorterQueueSorter().sortBuildableItems(items);
		}
		// Sort
		Collections.sort(items, new Comparator<BuildableItem>() {
			public int compare(BuildableItem o1, BuildableItem o2) {
				float o1weight = getCalculatedWeight(o1);
				float o2weight = getCalculatedWeight(o2);
				if (o1weight > o2weight) {
					return 1;
				}
				if (o1weight < o2weight) {
					return -1;
				}
				return (int) (o1.getInQueueSince() - o2.getInQueueSince());
			}
		});
	}

	/**
	 * Returned the calculated, cached, weight or calculates the weight if missing. Should only be
	 * called when the value should already be there, if the item is new {@link #onNewItem(Item)} is
	 * the method to call.
	 * 
	 * @param item the item to get the weight for
	 * @return the calculated weight
	 */
	private float getCalculatedWeight(BuildableItem item) {
		try {
			return item2weight.get(item.id);
		} catch (NullPointerException e) {
			onNewItem(item);
			return item2weight.get(item.id);
		}
	}

	public void onNewItem(Item item) {
		final SorterStrategy prioritySorterStrategy = PrioritySorterConfiguration.get().getStrategy();
		final float weight = prioritySorterStrategy.onNewItem(item);
		item2weight.put(item.id, weight);
	}

	public void onLeft(LeftItem li) {
		final SorterStrategy prioritySorterStrategy = PrioritySorterConfiguration.get().getStrategy();
		Float weight = item2weight.remove(li.id);
		if (li.isCancelled()) {
			prioritySorterStrategy.onCanceledItem(li);
		} else {
			prioritySorterStrategy.onStartedItem(li, weight);
		}

	}

	static public AdvancedQueueSorter get() {
		return QueueSorter.all().get(AdvancedQueueSorter.class);
	}

}