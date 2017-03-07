package myeslib3.stack1.features.persistence;

import myeslib3.core.data.UnitOfWork;

@FunctionalInterface
public interface Journal<ID> {
  void append(ID targetId, UnitOfWork unitOfWork);
}