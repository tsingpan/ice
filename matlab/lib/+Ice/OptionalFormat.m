%{
**********************************************************************

Copyright (c) 2003-2017 ZeroC, Inc. All rights reserved.

This copy of Ice is licensed to you under the terms described in the
ICE_LICENSE file included in this distribution.

**********************************************************************
%}

classdef OptionalFormat < uint32
    enumeration
        F1 (0)
        F2 (1)
        F4 (2)
        F8 (3)
        Size (4)
        VSize (5)
        FSize (6)
        Class (7)
    end
end