package you.bs.shortest.online;

import you.bs.shortest.common.Level;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import org.jheaps.AddressableHeap;
import org.jheaps.array.DaryArrayAddressableHeap;

public class IndexFrontier {
    final DaryArrayAddressableHeap<Double, Record> heap = new DaryArrayAddressableHeap<>(4);
    final Int2ObjectOpenHashMap<AddressableHeap.Handle<Double, Record>> seen = new Int2ObjectOpenHashMap<>();
    Level[][] levels;

    IndexFrontier(Level[][] levels, int index) {
        this.levels = levels;
        AddressableHeap.Handle<Double, Record> node = heap.insert(0d, new Record(index, -1));
        seen.put(index, node);
    }

    AddressableHeap.Handle<Double, Record> deleteMin() {
        AddressableHeap.Handle<Double, Record> min = heap.deleteMin();
        for (Level up : levels[min.getValue().now]) {
            int index = up.getIndex();
            double weight = min.getKey() + up.getWeight();
            int down = min.getValue().now;
            AddressableHeap.Handle<Double, Record> node = seen.get(index);
            if (node == null) {
                node = heap.insert(weight, new Record(index, down));
                seen.put(up.getIndex(), node);
            } else if (weight < node.getKey()) {
                node.decreaseKey(weight);
                node.getValue().down = down;
            }
        }
        return min;
    }

    double getWeight(int index) {
        AddressableHeap.Handle<Double, Record> node = seen.get(index);
        if (node != null) {
            return node.getKey();
        }
        return Double.POSITIVE_INFINITY;
    }

    @AllArgsConstructor
    static class Record {
        int now;
        int down;
    }
}
