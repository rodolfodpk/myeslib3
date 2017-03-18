package myeslib3.stack1;

import myeslib3.core.data.UnitOfWork;
import myeslib3.stack.EventsProjector;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

// https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.2/html/Apache_Camel_Development_Guide/MsgRout-LoadBalancer.html#MsgRout-LoadBalancer-Sticky
// https://github.com/apache/camel/blob/master/camel-core/src/main/java/org/apache/camel/processor/loadbalancer/StickyLoadBalancer.java
// http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html
/*
	Using a SynchronousQueue to propagate exceptions and use Camel idempotency within route class
  A good idea is to use it together with http://camel.apache.org/throttler.html
 */

// https://blog.garage-coding.com/2016/05/25/redesigning-notification.html
//  http://www.davsclaus.com/2013/08/apache-camel-212-backoff-support-for.html
// https://omniti.com/seeds/stop-collaborate-and-listen-notify - real time msg processing and batch msg pooling
// (or manually triggered) when consumer kept offline for a while

//  commands idempotency and uow idempotency and commands queue and uow queue vs
// idempotency na mesma tabela da queue com key da UnitOfWork e timestamp de processing time
public class Stack1EventsProjector implements EventsProjector {

	final Consumer<UnitOfWork> projectionSideEffect;
	final Cache<String, Integer> cache; // this has very short TTL requirements (just to avoid collision)

	final List<ExecutorService> executors;

	public Stack1EventsProjector(int executorsPoolSize,
															 Consumer<UnitOfWork> projectionSideEffect,
															 Cache<String, Integer> cache) {

		this.executors = new ArrayList<>(executorsPoolSize);
		this.projectionSideEffect = projectionSideEffect;
		this.cache = cache;
		for (int i = 0; i < executorsPoolSize; i++) {
			final ExecutorService executorService = new ThreadPoolExecutor(1, 1,
							1L, TimeUnit.MINUTES,
							new SynchronousQueue<>());
			executors.add(executorService);
		}
	}

	public void submit(final UnitOfWork unitOfWork) {
		final Integer targetExecutor = cache.get(unitOfWork.getAggregateRootId(), instanceId ->
						ThreadLocalRandom.current().nextInt(0, executors.size() + 1));
		cache.put(unitOfWork.getAggregateRootId(), targetExecutor);
		final ExecutorService e = executors.get(targetExecutor);
		e.submit(() -> projectionSideEffect.accept(unitOfWork));
	}

	// Not soo necessary because each executor operates on a SynchronousQueue
	void shutdown() {
		for (ExecutorService e : executors) {
			e.shutdown();
		}
	}

}

