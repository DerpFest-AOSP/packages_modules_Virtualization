/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.system.virtualmachine;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages {@link VirtualMachine} objects created for an application.
 *
 * @hide
 */
public class VirtualMachineManager {
    private final Context mContext;

    private VirtualMachineManager(Context context) {
        mContext = context;
    }

    static Map<Context, WeakReference<VirtualMachineManager>> sInstances = new WeakHashMap<>();

    /** Returns the per-context instance. */
    public static VirtualMachineManager getInstance(Context context) {
        synchronized (sInstances) {
            VirtualMachineManager vmm =
                    sInstances.containsKey(context) ? sInstances.get(context).get() : null;
            if (vmm == null) {
                vmm = new VirtualMachineManager(context);
                sInstances.put(context, new WeakReference(vmm));
            }
            return vmm;
        }
    }

    /** A lock used to synchronize the creation of virtual machines */
    private static final Object sCreateLock = new Object();

    /**
     * Creates a new {@link VirtualMachine} with the given name and config. Creating a virtual
     * machine with the same name as an existing virtual machine is an error. The existing virtual
     * machine has to be deleted before its name can be reused. Every call to this methods creates a
     * new (and different) virtual machine even if the name and the config are the same as the
     * deleted one.
     */
    public VirtualMachine create(String name, VirtualMachineConfig config)
            throws VirtualMachineException {
        synchronized (sCreateLock) {
            return VirtualMachine.create(mContext, name, config);
        }
    }

    /**
     * Returns an existing {@link VirtualMachine} with the given name. Returns null if there is no
     * such virtual machine.
     */
    public VirtualMachine get(String name) throws VirtualMachineException {
        return VirtualMachine.load(mContext, name);
    }

    /**
     * Returns an existing {@link VirtualMachine} if it exists, or create a new one. If the virtual
     * machine exists, and config is not null, the virtual machine is re-configured with the new
     * config. However, if the config is not compatible with the original config of the virtual
     * machine, exception is thrown.
     */
    public VirtualMachine getOrCreate(String name, VirtualMachineConfig config)
            throws VirtualMachineException {
        VirtualMachine vm;
        synchronized (sCreateLock) {
            vm = get(name);
            if (vm == null) {
                return create(name, config);
            }
        }

        if (config != null) {
            // Can throw VirtualMachineException is the new config is not compatible with the
            // old config.
            vm.setConfig(config);
        }
        return vm;
    }
}
