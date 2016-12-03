package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IValuePrinter;
import openmods.calc.NullaryFunction;
import openmods.calc.UnaryFunction;
import openmods.utils.Stack;

public class OptionalTypeFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;
	private final TypedValue absentValue;
	private final TypedValue typeValue;

	public OptionalTypeFactory(TypedValue nullValue) {
		this.domain = nullValue.domain;
		this.nullValue = nullValue;
		this.absentValue = domain.create(IComposite.class, new Absent());
		this.typeValue = domain.create(IComposite.class, createOptionalType());
	}

	private interface OptionalTrait extends ICompositeTrait {
		public boolean isPresent();

		public TypedValue getValue();
	}

	private TypedValue wrap(ICallable<TypedValue> callable) {
		return domain.create(ICallable.class, callable);
	}

	private static final String MEMBER_MAP = "map";
	private static final String MEMBER_OR_CALL = "orCall";
	private static final String MEMBER_OR = "or";
	private static final String MEMBER_GET = "get";

	private class Absent extends SimpleComposite implements OptionalTrait, CompositeTraits.Printable, CompositeTraits.Truthy, CompositeTraits.Structured, CompositeTraits.Typed {
		private final Map<String, TypedValue> members;

		public Absent() {
			final Builder<String, TypedValue> members = ImmutableMap.builder();

			members.put(MEMBER_GET, wrap(new NullaryFunction<TypedValue>() {
				@Override
				protected TypedValue call() {
					throw new IllegalStateException("Value not present");
				}
			}));

			members.put(MEMBER_OR, wrap(new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					return value;
				}
			}));

			members.put(MEMBER_OR_CALL, wrap(new FixedCallable<TypedValue>(1, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final TypedValue value = frame.stack().pop();
					if (!TypedCalcUtils.tryCall(frame, value, Optional.of(1), Optional.of(0)))
						throw new IllegalArgumentException("Value is not callable: " + value);

				}
			}));

			members.put(MEMBER_MAP, wrap(new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					Preconditions.checkArgument(TypedCalcUtils.isCallable(value), "Value is not callable: %s", value);
					return absentValue;
				}
			}));

			members.put("isPresent", domain.create(Boolean.class, Boolean.FALSE));

			this.members = members.build();
		}

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public TypedValue getValue() {
			throw new IllegalStateException("Value not present");
		}

		@Override
		public String type() {
			return "Optional";
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			return Optional.fromNullable(members.get(component));
		}

		@Override
		public boolean isTruthy() {
			return false;
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			return "Optional.Absent()";
		}

		@Override
		public TypedValue getType() {
			return typeValue;
		}

		// no need for explicit equatable for now, since there's only one value of this type
	}

	private class Present extends SimpleComposite implements OptionalTrait, CompositeTraits.Printable, CompositeTraits.Truthy, CompositeTraits.Structured, CompositeTraits.Equatable, CompositeTraits.Typed {
		private final TypedValue value;

		private final Map<String, TypedValue> members;

		public Present(TypedValue value) {
			this.value = value;

			final Builder<String, TypedValue> members = ImmutableMap.builder();

			members.put(MEMBER_GET, wrap(new NullaryFunction<TypedValue>() {
				@Override
				protected TypedValue call() {
					return Present.this.value;
				}
			}));

			members.put(MEMBER_OR, wrap(new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue callable) {
					return Present.this.value;
				}
			}));

			members.put(MEMBER_OR_CALL, wrap(new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue callable) {
					Preconditions.checkArgument(TypedCalcUtils.isCallable(callable), "Value is not callable: %s", callable);
					return Present.this.value;
				}
			}));

			members.put(MEMBER_MAP, wrap(new FixedCallable<TypedValue>(1, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 1);
					final Stack<TypedValue> stack = executionFrame.stack();
					final TypedValue callable = stack.pop();

					stack.push(Present.this.value);
					if (!TypedCalcUtils.tryCall(executionFrame, callable, Optional.of(1), Optional.of(1)))
						throw new IllegalArgumentException("Value is not callable: " + callable);

					final TypedValue result = stack.pop();

					if (!stack.isEmpty())
						throw new IllegalStateException("Values left on stack: " + Lists.newArrayList(stack));

					stack.push(domain.create(IComposite.class, new Present(result)));
				}
			}));

			this.members = members.build();
		}

		@Override
		public boolean isPresent() {
			return true;
		}

		@Override
		public TypedValue getValue() {
			return value;
		}

		@Override
		public String type() {
			return "Optional";
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			return Optional.fromNullable(members.get(component));
		}

		@Override
		public boolean isTruthy() {
			return true;
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			return "Optional.Present(" + printer.str(value) + ")";
		}

		@Override
		public boolean isEqual(TypedValue value) {
			if (value.value == this) return true;

			if (value.value instanceof Present) {
				final Present other = (Present)value.value;
				return other.value.equals(this.value);
			}

			return false;
		}

		@Override
		public TypedValue getType() {
			return typeValue;
		}

	}

	private IComposite createPresentConstructor() {
		class PresentConstructor extends UnaryFunction<TypedValue> implements CompositeTraits.Callable {
			@Override
			protected TypedValue call(TypedValue value) {
				return domain.create(IComposite.class, new Present(value));
			}
		}

		return new MappedComposite.Builder()
				.put(CompositeTraits.Callable.class, new PresentConstructor())
				.put(CompositeTraits.Decomposable.class, new CompositeTraits.Decomposable() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(TypedValue input, int variableCount) {
						Preconditions.checkArgument(variableCount == 1, "Invalid number of values to unpack, expected 1 got %s", variableCount);
						final Optional<OptionalTrait> trait = TypedCalcUtils.tryGetTrait(input, OptionalTrait.class);
						if (!trait.isPresent()) return Optional.absent();
						final OptionalTrait optionalInfo = trait.get();
						if (!optionalInfo.isPresent()) return Optional.absent();
						final List<TypedValue> result = Lists.newArrayList(optionalInfo.getValue());
						return Optional.of(result);
					}
				})
				.build("<constructor Optional.Present>");
	}

	private static final Optional<List<TypedValue>> ABSENT_MATCH = Optional.of(Collections.<TypedValue> emptyList());

	private IComposite createAbsentConstructor() {
		class AbsentConstructor extends NullaryFunction<TypedValue> implements CompositeTraits.Callable {
			@Override
			protected TypedValue call() {
				return absentValue;
			}
		}

		return new MappedComposite.Builder()
				.put(CompositeTraits.Callable.class, new AbsentConstructor())
				.put(CompositeTraits.Decomposable.class, new CompositeTraits.Decomposable() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(TypedValue input, int variableCount) {
						Preconditions.checkArgument(variableCount == 0, "Invalid number of values to unpack, expected none got %s", variableCount);
						final Optional<OptionalTrait> trait = TypedCalcUtils.tryGetTrait(input, OptionalTrait.class);
						if (!trait.isPresent()) return Optional.absent();
						final OptionalTrait optionalInfo = trait.get();
						if (optionalInfo.isPresent()) return Optional.absent();
						else return ABSENT_MATCH;
					}
				})
				.build("<constructor Optional.Absent>");
	}

	private IComposite createOptionalType() {
		final Map<String, TypedValue> methods = ImmutableMap.<String, TypedValue> builder()
				.put("from", wrap(new UnaryFunction<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue value) {
						if (value == nullValue)
							return absentValue;
						else
							return domain.create(IComposite.class, new Present(value));
					}
				}))
				.put("Present", domain.create(IComposite.class, createPresentConstructor()))
				.put("Absent", domain.create(IComposite.class, createAbsentConstructor()))
				.build();

		return new MappedComposite.Builder()
				.put(CompositeTraits.TypeMarker.class, new CompositeTraits.TypeMarker() {})
				.put(CompositeTraits.Structured.class, new CompositeTraits.Structured() {
					@Override
					public Optional<TypedValue> get(TypeDomain domain, String component) {
						return Optional.fromNullable(methods.get(component));
					}
				})
				.build("<type Optional>");
	}

	public TypedValue value() {
		return typeValue;
	}
}