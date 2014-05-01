// **********************************************************************
//
// Copyright (c) 2003-2014 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package IceInternal;

public abstract class Functional_TwowayCallbackVoidUE
    extends Functional_TwowayCallbackUE implements Ice.TwowayCallbackVoidUE
{
    public Functional_TwowayCallbackVoidUE(
        Functional_VoidCallback responseCb, 
        Functional_GenericCallback1<Ice.UserException> userExceptionCb, 
        Functional_GenericCallback1<Ice.LocalException> localExceptionCb, 
        Functional_BoolCallback sentCb)
    {
        super(responseCb != null, userExceptionCb, localExceptionCb, sentCb);
        __responseCb = responseCb;
    }
    
    public void response()
    {
        if(__responseCb != null)
        {
            __responseCb.apply();
        }
    }
    
    private final Functional_VoidCallback __responseCb;
};