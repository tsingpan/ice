// **********************************************************************
//
// Copyright (c) 2003-2005 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package IceInternal;

public class BasicInputStream extends BasicStream
{
    public
    BasicInputStream(Instance instance, Ice.InputStream in)
    {
        super(instance);
        _in = in;
    }

    public Ice.InputStream _in;
}
