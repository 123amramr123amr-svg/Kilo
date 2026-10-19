/**
 * Permission manager for camera, microphone, and media library
 */

import { useState, useCallback } from 'react';
import { Camera } from 'expo-camera';
import * as MediaLibrary from 'expo-media-library';
import { Alert, Platform } from 'react-native';

export type PermissionStatus = 'undetermined' | 'granted' | 'denied' | 'unavailable';

interface Permissions {
  camera: PermissionStatus;
  microphone: PermissionStatus;
  mediaLibrary: PermissionStatus;
}

export const usePermissions = () => {
  const [permissions, setPermissions] = useState<Permissions>({
    camera: 'undetermined',
    microphone: 'undetermined',
    mediaLibrary: 'undetermined',
  });

  const requestCamera = useCallback(async () => {
    try {
      const { status } = await Camera.requestCameraPermissionsAsync();
      const newStatus: PermissionStatus =
        status === 'granted' ? 'granted' : status === 'denied' ? 'denied' : 'undetermined';
      setPermissions(prev => ({ ...prev, camera: newStatus }));
      return newStatus;
    } catch (error) {
      setPermissions(prev => ({ ...prev, camera: 'unavailable' }));
      return 'unavailable';
    }
  }, []);

  const requestMicrophone = useCallback(async () => {
    try {
      const { status } = await Camera.requestMicrophonePermissionsAsync();
      const newStatus: PermissionStatus =
        status === 'granted' ? 'granted' : status === 'denied' ? 'denied' : 'undetermined';
      setPermissions(prev => ({ ...prev, microphone: newStatus }));
      return newStatus;
    } catch (error) {
      setPermissions(prev => ({ ...prev, microphone: 'unavailable' }));
      return 'unavailable';
    }
  }, []);

  const requestMediaLibrary = useCallback(async () => {
    try {
      const { status } = await MediaLibrary.requestPermissionsAsync();
      const newStatus: PermissionStatus =
        status === 'granted' ? 'granted' : status === 'denied' ? 'denied' : 'undetermined';
      setPermissions(prev => ({ ...prev, mediaLibrary: newStatus }));
      return newStatus;
    } catch (error) {
      setPermissions(prev => ({ ...prev, mediaLibrary: 'unavailable' }));
      return 'unavailable';
    }
  }, []);

  const requestAll = useCallback(async () => {
    const [camera, microphone, mediaLibrary] = await Promise.all([
      requestCamera(),
      requestMicrophone(),
      requestMediaLibrary(),
    ]);
    return { camera, microphone, mediaLibrary };
  }, [requestCamera, requestMicrophone, requestMediaLibrary]);

  const checkCamera = useCallback(async () => {
    try {
      const { status } = await Camera.getCameraPermissionsAsync();
      const newStatus: PermissionStatus =
        status === 'granted' ? 'granted' : status === 'denied' ? 'denied' : 'undetermined';
      setPermissions(prev => ({ ...prev, camera: newStatus }));
      return newStatus;
    } catch {
      return 'unavailable';
    }
  }, []);

  const showPermissionAlert = useCallback((permission: string, message: string) => {
    Alert.alert(
      'Requires Permission',
      message,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Settings', onPress: () => {} },
      ]
    );
  }, []);

  return {
    permissions,
    requestCamera,
    requestMicrophone,
    requestMediaLibrary,
    requestAll,
    checkCamera,
    showPermissionAlert,
  };
};

export default usePermissions;