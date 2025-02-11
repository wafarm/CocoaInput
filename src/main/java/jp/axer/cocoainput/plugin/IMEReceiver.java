package jp.axer.cocoainput.plugin;

import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.Nullable;
import jp.axer.cocoainput.CocoaInput;
import jp.axer.cocoainput.util.ModLogger;
import jp.axer.cocoainput.util.PreeditFormatter;
import jp.axer.cocoainput.util.Rect;
import jp.axer.cocoainput.util.Tuple3;
import net.minecraft.client.Minecraft;

public abstract class IMEReceiver {

	private int length = 0;
	protected boolean cursorVisible = true;
	private boolean preeditBegin = false;
	protected int originalCursorPosition = 0;
	protected @Nullable BooleanSupplier allowTextDecoration;

	private void replaceMarkedText(String text, int pos, int len)
	{
		//ModLogger.log("replaceMarkedText() ... (new StringBuffer(\"" + this.getText() + "\").replace(" + pos + ", " + (pos + len) + ", \"" + text + "\")");
		this.setText((new StringBuffer(this.getText()))
			.replace(pos, pos + len, text).toString());
	}

	/*
	 * position1 length1は下線と強調変換のため必須 position2 length2は意味をなしてない
	 * positionの位置から文字数lengthの範囲という意味
	 */
	public void insertText(String aString, int position1, int length1) {//確定文字列 現状aString以外の引数は意味をなしてない
		//ModLogger.log("just comming:(\"" + aString + "\") now:(\"" + getText() + "\") length:" + length);
		if (!preeditBegin) {
			originalCursorPosition = this.getCursorPos();
		}
		preeditBegin = false;
		cursorVisible = true;
		replaceMarkedText("", originalCursorPosition, length);
		length = 0;
		this.setCursorPos(originalCursorPosition);
		this.setSelectionPos(originalCursorPosition);
		if (CocoaInput.config.isNativeCharTyped()) {
			this.insertTextNative(aString);
		} else {
			this.insertTextEmurated(aString);
		}
		/*
		if (aString.length() == 0) {
			this.setText((new StringBuffer(this.getText()))
					.replace(originalCursorPosition, originalCursorPosition + length, "").toString());
			length = 0;
			this.setCursorPos(originalCursorPosition);
			this.setSelectionPos(originalCursorPosition);
			return;
		}
		this.setText((new StringBuffer(this.getText()))
				.replace(originalCursorPosition, originalCursorPosition + length,
						aString.substring(0, aString.length()))
				.toString());
		length = 0;
		this.setCursorPos(originalCursorPosition + aString.length());
		this.notifyParent(this.getText());
		//owner.selectionEnd = owner.cursorPosition;
		 */
	}

	public void setMarkedText(String aString, int position1, int length1, int position2, int length2) {
		if (!preeditBegin) {
			originalCursorPosition = this.getCursorPos();
			preeditBegin = true;
		}
		int caretPosition;
		boolean hasCaret;
		String commitString;
		if (allowTextDecoration != null && !allowTextDecoration.getAsBoolean()) {
			hasCaret = false;
			caretPosition = 0;
			commitString = aString;
		}
		else if (CocoaInput.config.isAdvancedPreeditDraw()) {
			//ModLogger.log("PreeditFormatter.formatMarkedText(\"" + aString + "\", " + position1 + ", " + length1 + ")");
			int max = aString.length();
			Tuple3<String, Integer, Boolean> formattedText = PreeditFormatter.formatMarkedText(aString,
				position1 > max ? max : position1,
				position1 + length1 > max ? max - position1 : length1);
			commitString = formattedText._1();
			caretPosition = formattedText._2() + 4;//相対値
			hasCaret = formattedText._3();
		}
		else {
			hasCaret=true;
			caretPosition=0;
			commitString=PreeditFormatter.SECTION+"n"+aString+PreeditFormatter.SECTION+"r";
		}
		replaceMarkedText(commitString, originalCursorPosition, length);
		length = commitString.length();
		if (hasCaret) {
			this.cursorVisible = true;
			this.setCursorPos(originalCursorPosition + caretPosition);
			this.setSelectionPos(originalCursorPosition + caretPosition);
		} else {
			this.cursorVisible = false;
			this.setCursorInvisible();
			this.setCursorPos(originalCursorPosition);
			this.setSelectionPos(originalCursorPosition);
			//owner.selectionEnd=owner.cursorPosition;
		}
	}

	public abstract Rect getRect();

	abstract protected void setText(String text);

	abstract protected String getText();

	abstract protected void setCursorInvisible();

	abstract protected int getCursorPos();

	abstract protected void setCursorPos(int p);

	abstract protected void setSelectionPos(int p);

	protected void insertTextNative(String text) {
		for (char c : text.toCharArray()) {
			Minecraft instance = Minecraft.getInstance();
			instance.keyboardHandler.charTyped(instance.getWindow().getWindow(), c, 0);
		}
	}

	protected void insertTextEmurated(String aString) {
		if (this.getCursorPos() <= this.getText().length()) {
			replaceMarkedText(aString.substring(0, aString.length()), this.getCursorPos(), 0);
		}
		length = 0;
		this.setCursorPos(this.getCursorPos() + aString.length());
		this.notifyParent(this.getText());
	}

	protected void notifyParent(String text) {
	};
}
