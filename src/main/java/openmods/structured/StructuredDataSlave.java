package openmods.structured;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import net.minecraft.network.PacketBuffer;
import openmods.structured.Command.ConsistencyCheck;
import openmods.structured.Command.ContainerInfo;
import openmods.structured.Command.Create;
import openmods.structured.Command.Delete;
import openmods.structured.Command.Reset;
import openmods.structured.Command.Update;

public abstract class StructuredDataSlave<C extends IStructureContainer<E>, E extends IStructureElement> extends StructuredData<C, E> {

	public static class ConsistencyCheckFailed extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ConsistencyCheckFailed(String message) {
			super(message);
		}

		public ConsistencyCheckFailed(String format, Object... args) {
			super(String.format(format, args));
		}

		public ConsistencyCheckFailed(Throwable cause, String format, Object... args) {
			super(String.format(format, args), cause);
		}
	}

	public final IStructureContainerFactory<C> factory;

	protected StructuredDataSlave(IStructureContainerFactory<C> factory, IStructureObserver<C, E> observer) {
		super(observer);
		this.factory = factory;
	}

	protected StructuredDataSlave(IStructureContainerFactory<C> factory) {
		super();
		this.factory = factory;
	}

	protected abstract void onConsistencyCheckFail();

	public void interpretCommandList(List<Command> commands) {
		Multimap<Integer, Integer> updatedContainers = HashMultimap.create();

		boolean isStructureUpdated = false;
		observer.onUpdateStarted();
		for (Command c : commands) {
			try {
				if (c.isEnd()) break;
				else if (c instanceof ConsistencyCheck) {
					final ConsistencyCheck msg = (ConsistencyCheck)c;

					SortedSet<Integer> containers = containerToElement.keySet();

					final int containerCount = containers.size();
					final int minContainerId = containerCount == 0? 0 : containers.first();
					final int maxContainerId = containerCount == 0? 0 : containers.last();
					final int elementCount = elements.size();
					final int minElementId = elementCount == 0? 0 : elements.firstKey();
					final int maxElementId = elementCount == 0? 0 : elements.lastKey();

					if (msg.containerCount != containerCount ||
							msg.minContainerId != minContainerId ||
							msg.maxContainerId != maxContainerId ||
							msg.elementCount != elementCount ||
							msg.minElementId != minElementId ||
							msg.maxElementId != maxElementId)
						throw new ConsistencyCheckFailed("Validation packet not matched");
				} else if (c instanceof Reset) {
					removeAll();
					isStructureUpdated = true;
				} else if (c instanceof Create) {
					final Create msg = (Create)c;

					SortedSet<Integer> elements = Sets.newTreeSet();

					for (ContainerInfo info : msg.containers) {
						SortedSet<Integer> newElementsId = createAndAddContainer(msg.containerPayload, info.type, info.id, info.start);
						elements.addAll(newElementsId);
						updatedContainers.putAll(info.id, newElementsId);
					}

					if (msg.containerPayload.readableBytes() != 0) throw new ConsistencyCheckFailed("Container payload not fully consumed");

					readElementPayload(elements, msg.elementPayload);
					isStructureUpdated = true;
				} else if (c instanceof Delete) {
					final Delete msg = (Delete)c;
					for (int i : msg.idList)
						removeContainer(i);
					isStructureUpdated = true;
				} else if (c instanceof Update) {
					final Update msg = (Update)c;
					readElementPayload(msg.idList, msg.elementPayload);

					for (int elementId : msg.idList) {
						int containerId = elementToContainer.get(elementId);
						if (containerId == NULL) throw new ConsistencyCheckFailed("Orphaned element %d", elementId);
						updatedContainers.put(containerId, elementId);
					}
				}
			} catch (ConsistencyCheckFailed e) {
				onConsistencyCheckFail();
				break;
			}
		}

		if (isStructureUpdated) observer.onStructureUpdate();

		for (Map.Entry<Integer, Collection<Integer>> e : updatedContainers.asMap().entrySet()) {
			final Integer containerId = e.getKey();
			final C container = containers.get(containerId);
			observer.onContainerUpdated(containerId, container);

			for (Integer elementId : e.getValue()) {
				final E element = elements.get(elementId);
				observer.onElementUpdated(containerId, container, elementId, element);
			}
		}

		if (!updatedContainers.isEmpty()) observer.onDataUpdate();

		observer.onUpdateFinished();
	}

	private SortedSet<Integer> createAndAddContainer(PacketBuffer input, int type, int containerId, int start) {
		C container = factory.createContainer(type);
		if (container instanceof ICustomCreateData) ((ICustomCreateData)container).readCustomDataFromStream(input);
		if (containerToElement.containsEntry(containerId, start)) throw new ConsistencyCheckFailed("Container %d already exists", containerId);
		addContainer(containerId, container, start);
		return containerToElement.get(containerId);
	}

	private void readElementPayload(SortedSet<Integer> ids, PacketBuffer input) {
		try {
			for (Integer id : ids) {
				final E element = elements.get(id);
				if (element == null) throw new ConsistencyCheckFailed("Element %d not found", id);
				element.readFromStream(input);
			}

			if (input.readableBytes() != 0) throw new ConsistencyCheckFailed("Element payload not fully consumed");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
