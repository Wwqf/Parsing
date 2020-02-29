package fin;


import logger.Log;

import java.io.*;

public class BufferIO {
	public static final char EOF = '\0';
	public static boolean stopLexicalAnalysis = false;

	// Buffer number
	private static final int TAG_ONE = 1;
	private static final int TAG_TWO = 2;
	private int currentTag = TAG_ONE;


	// File operations
	private final BufferedReader reader;
	// Buffer
	private final int bufferSize;
	private char[] bufferOne, bufferTwo;

	private int lexemeBegin = 0;
	private int forward = -1;

	private int readEachLength = 0;

	private BufferIO(Builder builder) {
		this.reader = builder.reader;
		this.bufferSize = builder.bufferSize;
		bufferOne = new char[builder.bufferSize];
		bufferTwo = new char[builder.bufferSize];

		// Temporarily replace the EOF mark.
		bufferOne[bufferSize - 1] = EOF;
		bufferTwo[bufferSize - 1] = EOF;

		loadBuffer(currentTag);
	}

	// 将指针移至下一个词素，并返回当前词素的字符串
	public String nextMorpheme() {
		StringBuilder builder = new StringBuilder();
		if (lexemeBegin > forward) {
			// 两个指针不在同一个缓冲区, 先读取lexemeBegin所在缓冲区的数据

			char[] buffer;
			if (currentTag == TAG_ONE) {
				buffer = bufferTwo;
			} else buffer = bufferOne;

			char c;
			while ((c = buffer[lexemeBegin++]) != EOF) {
				builder.append(c);
			}

			lexemeBegin = 0;
			if (currentTag == TAG_ONE) {
				buffer = bufferOne;
			} else buffer = bufferTwo;

			while (lexemeBegin < forward) {
				builder.append(buffer[lexemeBegin++]);
			}
		} else {
			char[] buffer = getCurrentBufferReference();
			while (lexemeBegin < forward) {
				builder.append(buffer[lexemeBegin++]);
			}
		}
		forward--;
		return builder.toString();
	}

	/**
	 * 正常情况返回forward标记，有可能返回EOF标记，注意判断
	 * @return buffer[forward]
	 */
	public char nextChar() {
		char[] buffer = getCurrentBufferReference();
		char c = buffer[++forward];

		if (c == EOF) {
			if (forward == bufferSize - 1) {
				if (currentTag == TAG_ONE) {
					loadBuffer(TAG_TWO);
					forward = -1;
				} else {
					loadBuffer(TAG_ONE);
					forward = -1;
				}
			} else {
				BufferIO.stopLexicalAnalysis = true;
			}
		}
		return c;
	}

	private void loadBuffer(int Tag) {
		currentTag = Tag;
		char[] buffer = getCurrentBufferReference();

		try {
			int resultCode = reader.read(buffer, 0, bufferSize - 1);

			if (resultCode != -1) {
				readEachLength = resultCode;
				buffer[resultCode] = EOF;
			} else {
				close();
				Log.debug("The file has been read.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close() {
		try {
			if (reader != null)
				reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private char[] getCurrentBufferReference() {
		if (currentTag == TAG_ONE) return bufferOne;
		return bufferTwo;
	}

	public String getCurrentBufferString() {
		if (currentTag == TAG_ONE) return new String(bufferOne, lexemeBegin, readEachLength - lexemeBegin);
		else return new String(bufferTwo, lexemeBegin, readEachLength - lexemeBegin);
	}

	public static final class Builder {
		private BufferedReader reader = null;
		private int bufferSize = 4096;

		public Builder() { }

		public Builder setBufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
			return this;
		}

		public Builder setFilePath(String filePath) {
			try {
				File file = new File(filePath);
				if (!file.exists()) {
					Log.error("filePath is wrong.");
					System.exit(1);
				}

				this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			return this;
		}

		public BufferIO build() {
			return new BufferIO(this);
		}
	}
}
