package myeslib3.stack.persistence;

import myeslib3.core.data.UnitOfWork;

@FunctionalInterface
public interface Journal {
  void append(UnitOfWork unitOfWork);
}