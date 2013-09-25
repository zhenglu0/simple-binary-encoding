/* -*- mode: java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*- */
/*
 * Copyright 2013 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.sbe.ir;

import uk.co.real_logic.sbe.PrimitiveType;

import java.nio.ByteOrder;

/**
 * Class to encapsulate a token of information for the message schema stream. This Intermediate Representation (IR)
 * is intended to be language, schema, platform independent.
 * <p>
 * Processing and optimization could be run over a list of IrNodes to perform various functions
 * <ul>
 *     <li>re-ordering of fields based on size</li>
 *     <li>padding of fields in order to provide expansion room</li>
 *     <li>computing offsets of individual fields</li>
 *     <li>etc.</li>
 * </ul>
 *<p>
 * IR could be used to generate code or other specifications. It should be possible to do the
 * following:
 * <ul>
 *     <li>generate a FIX/SBE schema from IR</li>
 *     <li>generate an ASN.1 spec from IR</li>
 *     <li>generate a GPB spec from IR</li>
 *     <li>etc.</li>
 * </ul>
 *<p>
 * IR could be serialized to storage or network via code generated by SBE. Then read back in to
 * a List of IrNodes.
 *<p>
 *
 * The entire IR of an entity is a {@link java.util.List} of Token objects. The order of this list is
 * very important. Encoding of fields is done by nodes pointing to specific encoding {@link PrimitiveType}
 * objects. Each encoding node contains size, offset, byte order, and {@link Metadata}. Entities relevant
 * to the encoding such as fields, messages, repeating groups, etc. are encapsulated in the list as nodes
 * themselves. Although, they will in most cases never be serialized. The boundaries of these entities
 * are delimited by START and END {@link Signal} values in the node {@link Metadata}.
 * A list structure like this allows for each concatenation of encodings as well as easy traversal.
 *<p>
 * An example encoding of a message header might be like this.
 * <ul>
 *     <li>Token 0 - Signal = MESSAGE_START, id = 100</li>
 *     <li>Token 1 - Signal = FIELD_START, id = 25</li>
 *     <li>Token 2 - Signal = NONE, PrimitiveType = uint32, size = 4, offset = 0</li>
 *     <li>Token 3 - Signal = FIELD_END</li>
 *     <li>Token 4 - Signal = MESSAGE_END</li>
 * </ul>
 *<p>
 * Specific nodes have IR IDs. These IDs are used to cross reference entities that have a relationship. Such as
 * length fields for variable length data elements, and entry count fields for repeating groups.
 * {@link Metadata#getIrId()} can be used to return the nodes IR ID. While {@link Metadata#getXRefIrId()} can
 * be used to return the nodes cross reference IR ID. Cross referencing is always two-way.
 */
public class Token
{
    /** Size not determined */
    public static final int VARIABLE_SIZE = -1;

    /** Offset not computed or set */
    public static final int UNKNOWN_OFFSET = -1;

    private final PrimitiveType primitiveType;
    private final int size;
    private final int offset;
    private final ByteOrder byteOrder;
    private final Metadata metadata;

    /**
     * Construct an {@link Token} by providing values for all fields.
     *
     * @param primitiveType representing this node or null.
     * @param size          of the node in bytes.
     * @param offset        within the {@link uk.co.real_logic.sbe.xml.Message}.
     * @param byteOrder     for the encoding.
     * @param metadata      for the {@link uk.co.real_logic.sbe.xml.Message}.
     */
    public Token(final PrimitiveType primitiveType,
                 final int size,
                 final int offset,
                 final ByteOrder byteOrder,
                 final Metadata metadata)
    {
        this.primitiveType = primitiveType;
        this.size = size;
        this.offset = offset;
        this.byteOrder = byteOrder;
        this.metadata = metadata;

        if (metadata == null)
        {
            throw new RuntimeException("metadata of Token must not be null");
        }
    }

    /**
     * Construct a default {@link Token} based on {@link Metadata} with defaults for other fields.
     *
     * @param metadata for this node.
     */
    public Token(final Metadata metadata)
    {
        this.primitiveType = null;
        this.size = 0;
        this.offset = 0;
        this.byteOrder = null;
        this.metadata = metadata;

        if (metadata == null)
        {
            throw new RuntimeException("metadata of Token must not be null");
        }
    }

    /**
     * @return the primitive type of this node. This value is only relevant for nodes that are encodings.
     */
    public PrimitiveType getPrimitiveType()
    {
        return primitiveType;
    }

    /**
     * @return the size of this node. A value of 0 means the node has no size when encoded. A value of
     *        {@link Token#VARIABLE_SIZE} means this node represents a variable length field.
     */
    public int size()
    {
        return size;
    }

    /**
     * @return the offset of this node. A value of 0 means the node has no relevant offset. A value of
     *         {@link Token#UNKNOWN_OFFSET} means this nodes true offset is dependent on variable length
     *         fields ahead of it in the encoding.
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Return the {@link Metadata} of the {@link Token}.
     *
     * @return metadata of the {@link Token}
     */
    public Metadata getMetadata()
    {
        return metadata;
    }

    /**
     * Return the byte order of this field.
     */
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Signal the {@link Token} purpose. These signals start/end various entities such as
     * fields, composites, messages, repeating groups, enumerations, bitsets, etc.
     */
    public enum Signal
    {
        /** Denotes the start of a message */
        MESSAGE_START,

        /** Denotes the end of a message */
        MESSAGE_END,

        /** Denotes the start of a composite */
        COMPOSITE_START,

        /** Denotes the end of a composite */
        COMPOSITE_END,

        /** Denotes the start of a field */
        FIELD_START,

        /** Denotes the end of a field */
        FIELD_END,

        /** Denotes the start of a repeating group */
        GROUP_START,

        /** Denotes the end of a repeating group */
        GROUP_END,

        /** Denotes the start of an enumeration */
        ENUM_START,

        /** Denotes a value of an enumeration */
        ENUM_VALUE,

        /** Denotes the end of an enumeration */
        ENUM_END,

        /** Denotes the start of a bitset */
        SET_START,

        /** Denotes a bit value (choice) of a bitset */
        SET_CHOICE,

        /** Denotes the end of a bitset */
        SET_END,

        /** Denotes the {@link Token} is a basic encoding node */
        NONE
    }
}
