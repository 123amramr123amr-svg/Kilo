/**
 * Camera hook - manages Expo Camera, device capabilities, and capture
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import { useCameraPermissions } from 'expo-camera';
import { Platform, Alert } from 'react-native';
import { CameraType, CameraSettings, FlashMode } from '../types';
import { CAMERA_MODES } from '../constants/theme';

interface UseCameraReturn {
  cameraRef: React.RefObject<any>;
  hasPermission: boolean | null;
  type: CameraType;
  supportedTypes: CameraType[];
  mode: 'photo' | 'video';
  setMode: (mode: 'photo' | 'video') => void;
  zoom: number;
  setZoom: (zoom: number) => void;
  flash: FlashMode;
  setFlash: (flash: FlashMode) => void;
  isRecording: boolean;
  toggleCamera: () => void;
  takePhoto: () => Promise<string | null>;
  startRecording: () => Promise<void>;
  stopRecording: () => Promise<string | null>;
  deviceCapabilities: {
    manualControls: boolean;
    flash: boolean;
    zoom: number;
  };
  cameraSettings: CameraSettings;
  updateCameraSetting: <K extends keyof CameraSettings>(key: K, value: CameraSettings[K]) => void;
}

export const useCamera = (): UseCameraReturn => {
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [permissionStatus] = useCameraPermissions();
  const [type, setType] = useState<CameraType>('back');
  const [mode, setMode] = useState<'photo' | 'video'>('photo');
  const [zoom, setZoom] = useState(1);
  const [flash, setFlash] = useState<FlashMode>('off');
  const [isRecording, setIsRecording] = useState(false);
  const cameraRef = useRef<any>(null);
  const [deviceCapabilities, setDeviceCapabilities] = useState({
    manualControls: false,
    flash: true,
    zoom: 5,
  });
  const [cameraSettings, setCameraSettings] = useState<CameraSettings>({});

  const supportedTypes: CameraType[] = ['back', 'front'];

  if (permissionStatus) {
    setHasPermission(permissionStatus.status === 'granted');
  }
  const toggleCamera = useCallback(() => {
    setType(prev => (prev === 'back' ? 'front' : 'back'));
  }, []);

  const updateCameraSetting = useCallback(<K extends keyof CameraSettings>(
    key: K,
    value: CameraSettings[K]
  ) => {
    setCameraSettings(prev => ({ ...prev, [key]: value }));
  }, []);

  const takePhoto = useCallback(async () => {
    if (!cameraRef.current || mode !== 'photo') return null;
    try {
      const photo = await cameraRef.current.takePictureAsync({
        quality: 1,
        base64: false,
      });
      return photo.uri;
    } catch (error) {
      Alert.alert('خطأ في التقاط الصورة', 'لم يتمكن التطبيق من التقاط الصورة');
      return null;
    }
  }, [mode]);

  const startRecording = useCallback(async () => {
    if (!cameraRef.current || mode !== 'video') return;
    try {
      await cameraRef.current.recordAsync();
      setIsRecording(true);
    } catch (error) {
      Alert.alert('خطأ في التسجيل', 'لم يتمكن التطبيق من تسجيل الفيديو');
    }
  }, [mode]);

  const stopRecording = useCallback(async () => {
    if (!cameraRef.current || mode !== 'video') return null;
    try {
      const video = await cameraRef.current.stopRecording();
      setIsRecording(false);
      return video.uri;
    } catch (error) {
      setIsRecording(false);
      Alert.alert('خطأ في التوقف', 'فشل إيقاف التسجيل');
      return null;
    }
  }, [mode]);

  return {
    cameraRef,
    hasPermission,
    type,
    supportedTypes,
    mode,
    setMode,
    zoom,
    setZoom,
    flash,
    setFlash,
    isRecording,
    toggleCamera,
    takePhoto,
    startRecording,
    stopRecording,
    deviceCapabilities,
    cameraSettings,
    updateCameraSetting,
  };
};