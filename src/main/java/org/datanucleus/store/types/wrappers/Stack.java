/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.store.types.wrappers;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.store.types.SCOList;
import org.datanucleus.store.types.SCOListIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Stack object.
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class Stack extends java.util.Stack implements SCOList, Cloneable
{
    protected transient ObjectProvider ownerOP;
    protected transient AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.Stack delegate;

    /**
     * Constructor, using the ObjectProvider of the "owner" and the field name.
     * @param ownerOP The owner ObjectProvider
     * @param mmd Metadata for the member
     **/
    public Stack(ObjectProvider ownerOP, AbstractMemberMetaData mmd)
    {
        this.ownerOP = ownerOP;
        this.ownerMmd = mmd;
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param o The object to set from
     * @param forInsert Whether the object needs inserting in the datastore with this value
     * @param forUpdate Whether to update the datastore with this value
     */
    public void initialise(Object o, boolean forInsert, boolean forUpdate)
    {
        Collection c = (Collection)o;
        if (c != null)
        {
            delegate = new java.util.Stack(); // Make copy of container rather than using same memory
            delegate.addAll(c);
        }
        else
        {
            delegate = new java.util.Stack();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023003", 
                ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, true, false)));
        }
    }

    /**
     * Method to initialise the SCO for use.
     */
    public void initialise()
    {
        delegate = new java.util.Stack();
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023003", 
                ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, true, false)));
        }
    }

    // ----------------------- Implementation of SCO methods -------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public Object getValue()
    {
        return delegate;
    }

    /**
     * Method to effect the load of the data in the SCO.
     * Used when the SCO supports lazy-loading to tell it to load all now.
     */
    public void load()
    {
        // Always loaded
    }

    /**
     * Method to return if the SCO has its contents loaded. Returns true.
     * @return Whether it is loaded
     */
    public boolean isLoaded()
    {
        return true;
    }

    /**
     * Method to update an embedded element in this collection.
     * @param element The element
     * @param fieldNumber Number of field in the element
     * @param value New value for this field
     */
    public void updateEmbeddedElement(Object element, int fieldNumber, Object value)
    {
        // Just mark field in embedded owners as dirty
        makeDirty();
    }

    /**
     * Accessor for the field name.
     * @return The field name
     **/
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Accessor for the owner object.
     * @return The owner object
     **/
    public Object getOwner()
    {
        return (ownerOP != null ? ownerOP.getObject() : null);
    }

    /**
     * Method to unset the owner and field information.
     **/
    public synchronized void unsetOwner()
    {
        if (ownerOP != null)
        {
            ownerOP = null;
            ownerMmd = null;
        }
    }

    /**
     * Utility to mark the object as dirty
     **/
    public void makeDirty()
    {
        if (ownerOP != null)
        {
            ownerOP.makeDirty(ownerMmd.getAbsoluteFieldNumber());
        }
    }

    /**
     * Method to return a detached copy of the container.
     * Recurse sthrough the elements so that they are likewise detached.
     * @param state State of detachment state
     * @return The detached container
     */
    public Object detachCopy(FetchPlanState state)
    {
        java.util.Collection detached = new java.util.Stack();
        SCOUtils.detachCopyForCollection(ownerOP, toArray(), state, detached);
        return detached;
    }

    /**
     * Method to return an attached copy of the passed (detached) value. The returned attached copy
     * is a SCO wrapper. Goes through the existing elements in the store for this owner field and
     * removes ones no longer present, and adds new elements. All elements in the (detached)
     * value are attached.
     * @param value The new (collection) value
     */
    public void attachCopy(Object value)
    {
        java.util.Collection c = (java.util.Collection) value;

        // Attach all of the elements in the new list
        boolean elementsWithoutIdentity = SCOUtils.collectionHasElementsWithoutIdentity(ownerMmd);

        java.util.List attachedElements = new java.util.ArrayList(c.size());
        SCOUtils.attachCopyForCollection(ownerOP, c.toArray(), attachedElements, elementsWithoutIdentity);

        // Update the attached list with the detached elements
        SCOUtils.updateListWithListElements(this, attachedElements);
    }

    // ------------------- Implementation of Stack methods ---------------------
 
    /**
     * Clone operator to return a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * </p>
     *
     * @return The cloned object
     */
    public Object clone()
    {
        return delegate.clone();
    }

    /**
     * Method to return if the list contains this element.
     * @param element The element
     * @return Whether it is contained
     **/
    public boolean contains(Object element)
    {
        return delegate.contains(element);
    }

    /**
     * Accessor for whether the Stack is empty.
     * @return Whether it is empty.
     **/
    public boolean empty()
    {
        return delegate.empty();
    }

    public synchronized boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    public synchronized int hashCode()
    {
        return delegate.hashCode();
    }

    /**
     * Method to retrieve an element no.
     * @param index The item to retrieve
     * @return The element at that position.
     **/
    public Object get(int index)
    {
        return delegate.get(index);
    }

    /**
     * Method to the position of an element.
     * @param element The element.
     * @return The position.
     **/
    public int indexOf(Object element)
    {
        return delegate.indexOf(element);
    }

    /**
     * Accessor for whether the Stack is empty.
     * @return Whether it is empty.
     **/
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Method to retrieve an iterator for the list.
     * @return The iterator
     **/
    public Iterator iterator()
    {
        return new SCOListIterator(this, ownerOP, delegate, null, true, -1);
    }

    /**
     * Method to retrieve a List iterator for the list.
     * @return The iterator
     **/
    public ListIterator listIterator()
    {
        return new SCOListIterator(this, ownerOP, delegate, null, true, -1);
    }

    /**
     * Method to retrieve a List iterator for the list from the index.
     * @param index The start point 
     * @return The iterator
     **/
    public ListIterator listIterator(int index)
    {
        return new SCOListIterator(this, ownerOP, delegate, null, true, index);
    }

    /**
     * Method to retrieve the last position of the element.
     * @param element The element
     * @return The last position of this element in the List.
     **/
    public int lastIndexOf(Object element)
    {
        return delegate.lastIndexOf(element);
    }

    /**
     * Method to retrieve the element at the top of the stack.
     * @return The element at the top of the stack
     **/
    public Object peek()
    {
        return delegate.peek();
    }

    /**
     * Accessor for the size of the Stack.
     * @return The size.
     **/
    public int size()
    {
        return delegate.size();
    }

    /**
     * Accessor for the subList of elements between from and to of the List
     * @param from Start index (inclusive)
     * @param to End index (exclusive) 
     * @return The subList
     **/
    public synchronized java.util.List subList(int from,int to)
    {
        return delegate.subList(from,to);
    }

    /**
     * Method to return the list as an array.
     * @return The array
     **/
    public synchronized Object[] toArray()
    {
        return delegate.toArray();
    }

    /**
     * Method to return the list as an array.
     * @param a The runtime types of the array being defined by this param
     * @return The array
     **/
    public synchronized Object[] toArray(Object a[])
    {
        return delegate.toArray(a);
    }

    /**
     * Method to add an element to a position in the Stack
     *
     * @param index The position
     * @param element The new element
     **/
    public void add(int index,Object element)
    {
        delegate.add(index, element);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationAdd(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add an element to the Stack
     *
     * @param element The new element
     * @return Whether it was added ok.
     **/
    public boolean add(Object element)
    {
        boolean success = delegate.add(element);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationAdd(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to add an element to the Stack
     *
     * @param element The new element
     **/
    public void addElement(Object element)
    {
        delegate.add(element);
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add a Collection to the Stack
     * @param elements The collection
     * @return Whether it was added ok.
     **/
    public boolean addAll(Collection elements)
    {
        boolean success = delegate.addAll(elements);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = elements.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationAdd(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to add a Collection to a position in the Stack
     * @param index Position to insert the collection.
     * @param elements The collection
     * @return Whether it was added ok.
     **/
    public boolean addAll(int index, Collection elements)
    {
        boolean success = delegate.addAll(index, elements);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = elements.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationAdd(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to clear the Stack
     **/
    public synchronized void clear()
    {
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = delegate.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationRemove(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (ownerOP != null && !delegate.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.hasDependentElement(ownerMmd))
            {
                java.util.List copy = new java.util.ArrayList(delegate);
                Iterator iter = copy.iterator();
                while (iter.hasNext())
                {
                    ownerOP.getExecutionContext().deleteObjectInternal(iter.next());
                }
            }
        }

        delegate.clear();

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to remove the top element in the stack and return it.
     * @return The top element that was in the Stack (now removed).
     **/
    public Object pop()
    {
        Object obj = delegate.pop();
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return obj;
    }

    /**
     * Method to push an element onto the stack and return it.
     *
     * @param element The element to push onto the stack.
     * @return The element that was pushed onto the Stack
     **/
    public Object push(Object element)
    {
        Object obj = delegate.push(element);
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return obj;
    }

    /**
     * Method to remove an element from the List
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     **/
    public synchronized boolean remove(Object element)
    {
        return remove(element, true);
    }

    /**
     * Method to remove an element from the List
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     **/
    public synchronized boolean remove(Object element, boolean allowCascadeDelete)
    {
        boolean success = delegate.remove(element);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationRemove(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (ownerOP != null && allowCascadeDelete)
        {
            // Cascade delete
            if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerOP.getExecutionContext().deleteObjectInternal(element);
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }

        return success;
    }

    /**
     * Method to remove a Collection of objects from the Stack
     * @param elements The Collection
     * @return Whether the collection of elements were removed
     **/
    public boolean removeAll(Collection elements)
    {
        boolean success = delegate.removeAll(elements);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = elements.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationRemove(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (ownerOP != null && elements != null && !elements.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.hasDependentElement(ownerMmd))
            {
                Iterator iter = elements.iterator();
                while (iter.hasNext())
                {
                    ownerOP.getExecutionContext().deleteObjectInternal(iter.next());
                }
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }

        return success;
    }

    /**
     * Method to remove an element from the Stack
     * @param element The element
     * @return Whether the element was removed
     **/
    public boolean removeElement(Object element)
    {
        return remove(element);
    }

    /**
     * Method to remove an element from the Stack
     * @param index The element position.
     * @return The object that was removed
     **/
    public Object remove(int index)
    {
        Object element = delegate.remove(index);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationRemove(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (ownerOP != null)
        {
            // Cascade delete
            if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerOP.getExecutionContext().deleteObjectInternal(element);
            }
        }

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return element;
    }

    /**
     * Method to remove an element from the Stack
     * @param index The element position.
     **/
    public void removeElementAt(int index)
    {
        remove(index);
    }

    /**
     * Method to remove all elements from the Stack.
     * Same as clear().
     **/
    public void removeAllElements()
    {
        clear();
    }

    /**
     * Method to retain a Collection of elements (and remove all others).
     * @param c The collection to retain
     * @return Whether they were retained successfully.
     **/
    public synchronized boolean retainAll(java.util.Collection c)
    {
        boolean success = delegate.retainAll(c);
        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Wrapper addition that allows turning off of the dependent-field checks
     * when doing the position setting. This means that we can prevent the deletion of
     * the object that was previously in that position. This particular feature is used
     * when attaching a list field and where some elements have changed positions.
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     */
    public Object set(int index, Object element, boolean allowDependentField)
    {
        Object obj = delegate.set(index, element);
        if (ownerOP != null && allowDependentField && !delegate.contains(obj))
        {
            // Cascade delete
            if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerOP.getExecutionContext().deleteObjectInternal(obj);
            }
        }

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return obj;
    }

    /**
     * Method to set the element at a position in the Stack
     *
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     **/
    public Object set(int index,Object element)
    {
        return set(index, element, true);
    }

    /**
     * Method to set the element at a position in the Stack
     *
     * @param element The new element
     * @param index The position
     **/
    public void setElementAt(Object element,int index)
    {
        delegate.setElementAt(element, index);
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * The writeReplace method is called when ObjectOutputStream is preparing
     * to write the object to the stream. The ObjectOutputStream checks whether
     * the class defines the writeReplace method. If the method is defined, the
     * writeReplace method is called to allow the object to designate its
     * replacement in the stream. The object returned should be either of the
     * same type as the object passed in or an object that when read and
     * resolved will result in an object of a type that is compatible with all
     * references to the object.
     * 
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        java.util.Stack stack = new java.util.Stack();
        stack.addAll(delegate);
        return stack;
    }
}