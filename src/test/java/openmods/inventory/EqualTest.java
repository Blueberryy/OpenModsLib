package openmods.inventory;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import openmods.inventory.StackEqualityTesterBuilder.IEqualityTester;
import org.junit.Assert;
import org.junit.Test;

public class EqualTest {

	static {
		Bootstrap.register();
	}

	private static void assertSymmetricEquals(IEqualityTester tester, ItemStack left, ItemStack right) {
		Assert.assertTrue(tester.isEqual(left, right));
		Assert.assertTrue(tester.isEqual(right, left));
	}

	private static void assertSymmetricNotEquals(IEqualityTester tester, ItemStack left, ItemStack right) {
		Assert.assertFalse(tester.isEqual(left, right));
		Assert.assertFalse(tester.isEqual(right, left));
	}

	@Test
	public void testEmptyIdentity() {
		ItemStack stack = new ItemStack(Utils.ITEM_A);

		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		IEqualityTester tester = builder.build();
		Assert.assertTrue(tester.isEqual(null, null));
		Assert.assertFalse(tester.isEqual(stack, null));
		Assert.assertFalse(tester.isEqual(null, stack));
		Assert.assertTrue(tester.isEqual(stack, stack));
	}

	@Test
	public void testEmpty() {
		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		IEqualityTester tester = builder.build();

		ItemStack stackA = new ItemStack(Utils.ITEM_A);
		ItemStack stackB = new ItemStack(Utils.ITEM_A);
		Assert.assertTrue(tester.isEqual(stackA, stackB));
	}

	@Test
	public void testCompositeIdentity() {
		ItemStack stack = new ItemStack(Utils.ITEM_A);

		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		builder.useItem();
		IEqualityTester tester = builder.build();

		Assert.assertTrue(tester.isEqual(null, null));
		Assert.assertFalse(tester.isEqual(stack, null));
		Assert.assertFalse(tester.isEqual(null, stack));
		Assert.assertTrue(tester.isEqual(stack, stack));
	}

	@Test
	public void testItemComparator() {
		ItemStack stackA1 = new ItemStack(Utils.ITEM_A, 1);
		ItemStack stackA2 = new ItemStack(Utils.ITEM_A, 2);
		ItemStack stackB = new ItemStack(Utils.ITEM_B, 2);

		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		builder.useItem();
		IEqualityTester tester = builder.build();

		assertSymmetricEquals(tester, stackA1, stackA2);
		assertSymmetricNotEquals(tester, stackA1, stackB);
		assertSymmetricNotEquals(tester, stackA2, stackB);
	}

	@Test
	public void testNBTComparator() {
		ItemStack stackAa1 = new ItemStack(Utils.ITEM_A, 1);
		{
			CompoundNBT stackTagCompound = new CompoundNBT();
			stackTagCompound.setBoolean("test", true);
			stackAa1.setTagCompound(stackTagCompound);
		}

		ItemStack stackAa2 = new ItemStack(Utils.ITEM_A, 2);
		{
			CompoundNBT stackTagCompound = new CompoundNBT();
			stackTagCompound.setBoolean("test", true);
			stackAa2.setTagCompound(stackTagCompound);
		}

		ItemStack stackAb = new ItemStack(Utils.ITEM_A);
		{
			CompoundNBT stackTagCompound = new CompoundNBT();
			stackTagCompound.setBoolean("test", false);
			stackAb.setTagCompound(stackTagCompound);
		}

		ItemStack stackAn = new ItemStack(Utils.ITEM_A);
		ItemStack stackBn = new ItemStack(Utils.ITEM_B);

		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		builder.useNBT();
		IEqualityTester tester = builder.build();

		assertSymmetricEquals(tester, stackAa1, stackAa2);
		assertSymmetricEquals(tester, stackAn, stackBn);

		assertSymmetricNotEquals(tester, stackAa1, stackAb);
		assertSymmetricNotEquals(tester, stackAa1, stackAn);
		assertSymmetricNotEquals(tester, stackAa1, stackBn);
	}

	@Test
	public void testCompositeComparator() {
		ItemStack stackA = new ItemStack(Utils.ITEM_A, 1, 10);
		ItemStack stackA1 = new ItemStack(Utils.ITEM_A, 1, 2);
		ItemStack stackA2 = new ItemStack(Utils.ITEM_A, 2, 1);

		ItemStack stackB = new ItemStack(Utils.ITEM_B, 1, 10);
		ItemStack stackB1 = new ItemStack(Utils.ITEM_B, 1, 2);
		ItemStack stackB2 = new ItemStack(Utils.ITEM_B, 2, 1);

		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		builder.useItem();
		builder.useSize();
		IEqualityTester tester = builder.build();

		assertSymmetricEquals(tester, stackA, stackA1);
		assertSymmetricEquals(tester, stackB, stackB1);

		assertSymmetricNotEquals(tester, stackA1, stackA2);
		assertSymmetricNotEquals(tester, stackA1, stackB1);
		assertSymmetricNotEquals(tester, stackA2, stackB1);
		assertSymmetricNotEquals(tester, stackA2, stackB2);
	}

	@Test
	public void testFullCompositeComparator() {
		ItemStack stackA = new ItemStack(Utils.ITEM_A, 1, 2);
		ItemStack stackA1 = new ItemStack(Utils.ITEM_A, 1, 10);
		ItemStack stackA2 = new ItemStack(Utils.ITEM_A, 1, 2);
		ItemStack stackA3 = new ItemStack(Utils.ITEM_A, 2, 1);

		ItemStack stackB = new ItemStack(Utils.ITEM_B, 1, 2);
		ItemStack stackB1 = new ItemStack(Utils.ITEM_B, 1, 10);
		ItemStack stackB2 = new ItemStack(Utils.ITEM_B, 1, 2);
		ItemStack stackB3 = new ItemStack(Utils.ITEM_B, 2, 1);

		StackEqualityTesterBuilder builder = new StackEqualityTesterBuilder();
		builder.useItem();
		builder.useSize();
		builder.useDamage();
		IEqualityTester tester = builder.build();

		final List<ItemStack> allStacks = Arrays.asList(stackA, stackA1, stackA2, stackA3, stackB, stackB1, stackB2, stackB3);

		for (ItemStack left : allStacks) {
			for (ItemStack right : allStacks) {
				boolean shouldBeEqual =
						left == right ||
								left == stackA && right == stackA2 ||
								right == stackA && left == stackA2 ||
								left == stackB && right == stackB2 ||
								right == stackB && left == stackB2;

				Assert.assertEquals(shouldBeEqual, tester.isEqual(left, right));
			}
		}
	}

}
