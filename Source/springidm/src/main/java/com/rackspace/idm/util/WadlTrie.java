package com.rackspace.idm.util;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WadlTrie {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private class Tree {
        private final HashMap<Object, Tree> children = new HashMap<Object, Tree>();
        private final Object                element;
        private final Tree                  p;
        private boolean                     wildcard = false;

        public Tree(final Object o, final Tree parent) {
            p = parent;
            element = o;
            final String s = element.toString();
            wildcard = s.startsWith("{") && s.endsWith("}");
        }

        public Tree add(final Object o) {
            if (children.containsKey(o)) {
                return children.get(o);
            }
            final Tree t = new Tree(o, this);
            children.put(o, t);
            return t;
        }

        Object find(final int index, final Object... o) {
            final int next = index + 1;

            // if we have reached the array size and at a node then this is the permission ID
            if( index == o.length) {
                if(children.isEmpty()) {
                    return element;
                }
            }

            // if this node is a path parameter place holder or exact match, iterate children
            if (isWildcard() || element.equals(o[index]) ) {
                for (final Object c : children.keySet()) {
                    final Object r = children.get(c).find(next, o);
                    if (r != null) {
                        return r;
                    }
                }
            }

            return null;
        }

        private boolean isWildcard() {
            return wildcard;
        }

        public Tree pop() {
            return p;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (final Object key : children.keySet()) {
                sb.append(children.get(key).toString(element.toString()));
            }
            return sb.toString();
        }

        private String toString(final String prefix) {
            final String newPrefix = prefix + " " + element.toString();

            final StringBuilder sb = new StringBuilder();
            for (final Object key : children.keySet()) {
                sb.append(children.get(key).toString(newPrefix));
            }
            if (children.size() == 0) {
                sb.append(newPrefix).append("\n");
            }
            return sb.toString();
        }
    }

    private class SaxHandler extends DefaultHandler {
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
        throws SAXException {
            if (qName.equals("ns2:resource")) {
                final String path = attributes.getValue("path");
                if (path.equals("/")) {
                    stack.addFirst(stack.peekFirst().add(path));
                } else {
                    final String[] paths = path.split("/");
                    Tree current = stack.peekFirst();
                    int index = 0;
                    while (index < paths.length) {
                        current = current.add(paths[index++]);
                    }
                    stack.addFirst(current);
                }
            }
            if (qName.equals("ns2:method")) {
                final String name = attributes.getValue("name");
                final String id = attributes.getValue("id");
                stack.peekFirst().add(name).add(id);
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if (qName.equals("ns2:resource")) {
                stack.removeFirst();
            }
        }
    }

    // the primary data structure
    Tree        trie  = new Tree("root", null);

    // used only during wadl parsing
    Deque<Tree> stack = new ArrayDeque<Tree>();
    {
        stack.addFirst(trie);
    }

    public WadlTrie(final InputStream is) {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(is, new SaxHandler());
        } catch (final Exception e) {
            logger.error("Error parsing Wadl");
        }
        // release reference - no longer needed after parse
        stack = null;
    }

    public Object getPermissionFor(final Object[] paths) {
        return trie.find(0, paths);
    }

    public Object getPermissionFor(final String method, final UriInfo uriInfo) {
        final List<String> paths = new ArrayList<String>();
        for(final PathSegment segment : uriInfo.getPathSegments()) {
            paths.add(segment.getPath());
        }
        paths.add(method);
        return getPermissionFor(paths.toArray());
    }

    @Override
    public String toString() {
        return trie.toString();
    }
}
