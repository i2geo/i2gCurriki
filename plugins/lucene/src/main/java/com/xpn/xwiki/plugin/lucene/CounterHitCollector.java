package com.xpn.xwiki.plugin.lucene;

import org.apache.lucene.search.HitCollector;

public class CounterHitCollector extends HitCollector {

    private int count=0;

    public void collect(int doc, float score) {
        count++;
    }

    public int getCount() {
        return count;
    }
}
