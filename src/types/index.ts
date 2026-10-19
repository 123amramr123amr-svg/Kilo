/**
 * Types and interfaces for LensAI application
 */

export type CameraMode = 'photo' | 'video' | 'pro';

export type CameraType = 'back' | 'front';

export type FlashMode = 'off' | 'on' | 'auto';

export type WhiteBalance =
  | 'auto'
  | 'cloudy'
  | 'daylight'
  | 'fluorescent'
  | 'tungsten';

export type SceneType =
  | 'portrait'
  | 'food'
  | 'landscape'
  | 'document'
  | 'night'
  | 'macro'
  | 'action'
  | 'unknown';

export interface CameraSettings {
  iso?: number;
  shutterSpeed?: number;
  focusDistance?: number;
  whiteBalance?: WhiteBalance;
  exposure?: number;
  zoom?: number;
  flash?: FlashMode;
}

export interface AiSuggestion {
  id: string;
  type: 'angle' | 'distance' | 'light' | 'composition' | 'scene' | 'stability' | 'focus';
  priority: 'high' | 'medium' | 'low';
  message: string;
  messageAr: string;
  icon: string;
  actionable: boolean;
  action?: () => void;
}

export interface SceneAnalysis {
  sceneType: SceneType;
  confidence: number;
  suggestedSettings: CameraSettings;
  suggestions: AiSuggestion[];
  detectedFaces?: number;
  isStable: boolean;
  brightness: number;
  qualityScore: number;
}

export interface PhotoInfo {
  id: string;
  uri: string;
  width: number;
  height: number;
  timestamp: number;
  mode: CameraMode;
  settings?: CameraSettings;
  location?: {
    latitude: number;
    longitude: number;
  };
  isEdited: boolean;
  originalUri?: string;
  edits?: PhotoEdits;
  isFavorite: boolean;
  albumId?: string;
}

export interface PhotoEdits {
  crop?: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  brightness: number;
  contrast: number;
  saturation: number;
  sharpness: number;
  tint?: number;
  warmth?: number;
}

export interface Album {
  id: string;
  name: string;
  nameAr: string;
  photoIds: string[];
  createdAt: number;
}

export interface AiSession {
  id: string;
  photoId: string;
  suggestions: AiSuggestion[];
  sceneAnalysis: SceneAnalysis;
  applied: boolean;
  timestamp: number;
}

export interface VideoInfo {
  id: string;
  uri: string;
  duration: number;
  width: number;
  height: number;
  timestamp: number;
  fps: number;
  resolution: string;
  size: number;
}

export interface AppSettings {
  aiAssistantEnabled: boolean;
  autoSaveOriginal: boolean;
  quality: 'low' | 'medium' | 'high' | 'ultra';
  format: 'jpeg' | 'heic';
  gridEnabled: boolean;
  levelEnabled: boolean;
  hapticsEnabled: boolean;
  language: 'ar' | 'en';
  theme: 'auto' | 'light' | 'dark';
  shutterSound: boolean;
  countdown: number; // seconds, 0 = disabled
}

export interface DeviceCapabilities {
  camera: boolean;
  frontCamera: boolean;
  backCamera: boolean;
  video: boolean;
  manualControls: boolean;
  flash: boolean;
  zoom: number; // max zoom
  supportedResolutions: string[];
  supportedFrameRates: number[];
  hasGyroscope: boolean;
}