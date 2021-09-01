package yoshikihigo.tinypdg.scorpio.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import yoshikihigo.tinypdg.pe.ProgramElementInfo;
import yoshikihigo.tinypdg.scorpio.data.ClonePairInfo;

public class BellonWriter extends Writer {

	public BellonWriter(final String path,
			final SortedSet<ClonePairInfo> clonepairs) {
		super(path, clonepairs);
	}

	@Override
	public void write() {

		try {

			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					this.path,true));
            //writer.write("start\n");
			for (final ClonePairInfo clonepair : this.clonepairs) {

				final SortedSet<ProgramElementInfo> elementsA = new TreeSet<ProgramElementInfo>(
						new LocationalComparator());
				final SortedSet<ProgramElementInfo> elementsB = new TreeSet<ProgramElementInfo>(
						new LocationalComparator());
				elementsA.addAll(clonepair.getLeftCodeFragment().getElements());
				elementsB.addAll(clonepair.getRightCodeFragment().getElements());

				writer.write(clonepair.pathA);
				writer.write("\t");
				writer.write(Integer.toString(elementsA.first().startLine));
				writer.write("\t");
				writer.write(Integer.toString(elementsA.last().endLine));
				writer.write("\t");
				writer.write(clonepair.authorA);
				writer.write("\t");
				writer.write(clonepair.pathB);
				writer.write("\t");
				writer.write(Integer.toString(elementsB.first().startLine));
				writer.write("\t");
				writer.write(Integer.toString(elementsB.last().endLine));
				writer.write("\t");
				writer.write(clonepair.authorB);
				writer.write("\t");
				writer.write("gap lines:");
				writer.write("\t");
				writer.write(this.generateGapsText(elementsA));
				writer.write("\t");
				writer.write(this.generateGapsText(elementsB));
				writer.newLine();
				writer.write("changed pdg size: ");
				writer.write(Integer.toString(clonepair.sizeA));
				writer.write("\t");
				writer.write("previous pdg size: ");
				writer.write(Integer.toString(clonepair.sizeB));
				writer.write("\t");
				writer.write("clone pair size: ");
				writer.write(Integer.toString(clonepair.size()));
				writer.write("\t");
				double f1 = new BigDecimal((float)clonepair.size()/clonepair.sizeA).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
				//double f2 = new BigDecimal((float)clonepair.size()/clonepair.sizeB).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
				writer.write("similar rate with A: ");
				writer.write(Double.toString(f1));
				//writer.write("\t");
				//writer.write("similar rate with B: ");
				//writer.write(Double.toString(f2));
				writer.newLine();
			}
			writer.write("end\n");
			writer.close();

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
}

	private String generateGapsText(final SortedSet<ProgramElementInfo> elements) {

		final SortedSet<Integer> lines = new TreeSet<Integer>();
		for (int line = elements.first().startLine; line <= elements.last().endLine; line++) {
			lines.add(line);
		}

		for (final ProgramElementInfo element : elements) {
			for (int line = element.startLine; line <= element.endLine; line++) {
				lines.remove(line);
			}
		}

		final StringBuilder text = new StringBuilder();
		if (lines.isEmpty()) {
			text.append("-");
		} else {
			for (final int line : lines) {
				text.append(Integer.toString(line));
				text.append(",");
			}
			text.deleteCharAt(text.length() - 1);
		}

		return text.toString();
	}

	class LocationalComparator implements Comparator<ProgramElementInfo> {

		@Override
		public int compare(final ProgramElementInfo o1,
				final ProgramElementInfo o2) {

			assert null != o1 : "\"o1\" is null.";
			assert null != o2 : "\"o2\" is null.";

			if (o1.startLine < o2.startLine) {
				return -1;
			} else if (o1.startLine > o2.startLine) {
				return 1;
			} else if (o1.endLine < o2.endLine) {
				return -1;
			} else if (o1.endLine > o2.endLine) {
				return 1;
			} else {
				return 0;
			}
		}

	}
}
