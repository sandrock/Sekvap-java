/*
 * Sekvap-java
 *
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <https://unlicense.org> and <https://github.com/sandrock/Sekvap-java/>
 */
 
package org.sdrk;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Sekvap serializer.
 * See https://github.com/sandrock/Sekvap-java for more information.
 */
public final class SekvapLanguage
{
	/// <summary>
	/// chars that need escaping when serializing a key.
	/// </summary>
	private static final char[] keyChars = new char[] { '=', ';', };

	/// <summary>
	/// chars that need escaping when serializing a value.
	/// </summary>
	private static final char[] valueChars = new char[] { ';', };

	public SekvapLanguage()
	{
	}

	public static void AddToResult(List<AbstractMap.SimpleEntry<String, String>> collection, String key, String value)
	{
		collection.add(new AbstractMap.SimpleEntry<String, String>(key, value));
	}

	/// <summary>
	/// Parse a string.
	/// </summary>
	/// <param name="value">the value to parse</param>
	/// <returns>a collection of key-value pairs</returns>
	public List<AbstractMap.SimpleEntry<String, String>> Parse(String value)
	{
		if (value == null)
			throw new IllegalArgumentException("value");

		List<AbstractMap.SimpleEntry<String, String>> result = new ArrayList<>();
		boolean isStart = true, isKey = true, isValue = false, isEnd = false;
		String capturedKey = null;
		int captureStartIndex = 0, captureEndIndex, captureLength;
		for (int i = captureStartIndex; i <= value.length(); i++)
		{
			char c, cp1;
			if (i == value.length())
			{
				c = Character.MIN_VALUE;
				cp1 = Character.MIN_VALUE;
				isEnd = true;
				captureEndIndex = i - 1;
				captureLength = i - captureStartIndex;
			}
			else
			{
				c = value.charAt(i);
				cp1 = (i + 1) < value.length() ? value.charAt(i + 1) : Character.MIN_VALUE;
				captureEndIndex = i;
				captureLength = i - captureStartIndex;
			}

			if (isStart)
			{
				if (c == ';' && cp1 == ';')
				{
					i++;
				}
				else if (c == ';' && cp1 != ';' || isEnd)
				{
					// end of start part
					AddToResult(result, "Value", value.substring(captureStartIndex, captureLength));
					i++;
					isStart = false;
					isKey = true;
					captureStartIndex = i;
					continue;
				}
			}
			
			if (isKey)
			{
				if ((c == '=' && cp1 == '=') || (c == ';' && cp1 == ';'))
				{
					i++;
				}
				else if (c == ';' && cp1 != ';' || isEnd)
				{
					if (isValue)
					{
						// end of start part
						String capturedValue = value.substring(captureStartIndex, captureLength);
						AddToResult(result, capturedKey, capturedValue);
					}
					else
					{
						capturedKey = value.substring(captureStartIndex, captureLength);
						AddToResult(result, capturedKey, null);
					}

					isValue = false;
					captureStartIndex = i + 1;
					continue;
				}
				else if (c == '=' && cp1 != '=')
				{
					// end of start part
					capturedKey = value.substring(captureStartIndex, captureLength);
					isValue = true;
					isStart = false;
					captureStartIndex = i + 1;
				}
			}
		}

		return result;
	}

	/// <summary>
	/// Serializes a collection of key-value pairs in Sekvap.
	/// </summary>
	/// <param name="values"></param>
	/// <returns>a Sekvap value</returns>
	public String Serialize(List<AbstractMap.SimpleEntry<String, String>> values)
	{
		if (values == null)
			throw new IllegalArgumentException("values");

		StringBuilder sb = new StringBuilder();
		int skip = -1;
        for (int i = 0; i < values.size(); i++) {
			String key = values.get(i).getKey();
			if ("Value".equals(key))
			{
				skip = i;
				EscapeValue(values.get(i).getValue(), sb);
				break;
			}
		}

        for (int i = 0; i < values.size(); i++) {
			if (skip == i)
				continue;
            
			sb.append(";");
			EscapeKey(values.get(i).getKey(), sb);
			sb.append("=");
			EscapeValue(values.get(i).getValue(), sb);
		}

		return sb.toString();
	}

	private static String EscapeKey(String value)
	{
		return EscapeKey(value, null);
	}

	private static int IndexOfAny(String value, char[] chars)
	{
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            for (int j = 0; j < chars.length; j++) {
                if (chars[j] == c) {
                    return i;
                }
            }
        }
        
        return -1;
	}

    private static boolean CharsContain(char[] chars, char c){
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == c) {
                return true;
            }
        }
        
        return false;
    }
    
	private static String EscapeKey(String value, StringBuilder sb)
	{
		if (value == null || value.length() == 0)
			throw new IllegalArgumentException("The value cannot be empty (value)");

		int pos = IndexOfAny(value, keyChars);
		if (pos < 0)
		{
			if (sb != null)
			{
				sb.append(value);
			}

			return value;
		}

		sb = sb != null ? sb : new StringBuilder(value.length() + 2);
		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			if (CharsContain(keyChars, c))
			{
				sb.append(c);
			}

			sb.append(c);
		}

		return sb.toString();
	}

	private static String EscapeValue(String value)
	{
		if (value == null)
			return null;

		return EscapeValue(value, null);
	}

	private static String EscapeValue(String value, StringBuilder sb)
	{
		if (value == null)
			return null;

		int pos = IndexOfAny(value, valueChars);
		if (pos < 0)
		{
			if (sb != null)
			{
				sb.append(value);
			}

			return value;
		}

		sb = sb != null ? sb : new StringBuilder(value.length() + 2);
		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			if (CharsContain(keyChars, c))
			{
				sb.append(c);
			}

			sb.append(c);
		}

		return sb.toString();
	}
}
