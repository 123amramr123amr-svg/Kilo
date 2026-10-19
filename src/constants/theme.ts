/**
 * Application constants, colors, and theme
 */

import { Platform, Dimensions } from 'react-native';
import { SceneType, AppSettings } from '../types';

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get('window');

export const COLORS = {
  // Primary palette - professional, calm
  primary: '#4A90D9',
  primaryDark: '#2E6BB0',
  primaryLight: '#8BB8F0',

  secondary: '#6C7B95',
  accent: '#5BB5A0',

  // Neutrals
  background: '#0D1117',
  backgroundElevated: '#161B22',
  surface: '#1C2128',
  surfaceHover: '#262C35',

  text: {
    primary: '#E6EDF3',
    secondary: '#8B949E',
    tertiary: '#6E7681',
    disabled: '#484F58',
  },

  border: '#30363D',
  borderLight: '#21262D',

  // Semantic
  success: '#3FB950',
  warning: '#D29922',
  error: '#F85149',
  info: '#58A6FF',

  // Camera specific
  overlay: 'rgba(0, 0, 0, 0.35)',
  overlayText: 'rgba(255, 255, 255, 0.9)',
  gridLine: 'rgba(255, 255, 255, 0.25)',
  levelGood: '#3FB950',
  levelBad: '#F85149',
  focusBox: '#4A90D9',
  shutter: '#FFFFFF',
  shutterActive: '#D29922',
};

export const SPACING = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
};

export const BORDER_RADIUS = {
  sm: 6,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
};

export const FONT_SIZES = {
  xs: 11,
  sm: 13,
  base: 15,
  lg: 17,
  xl: 20,
  '2xl': 24,
  '3xl': 30,
  '4xl': 36,
  '5xl': 48,
};

export const SHADOWS = {
  sm: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.3,
    shadowRadius: 3,
    elevation: 2,
  },
  md: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 4,
  },
  lg: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.5,
    shadowRadius: 12,
    elevation: 8,
  },
};

export const SCREEN = {
  width: SCREEN_WIDTH,
  height: SCREEN_HEIGHT,
  isSmall: SCREEN_WIDTH < 375,
  isMedium: SCREEN_WIDTH >= 375 && SCREEN_WIDTH < 768,
  isLarge: SCREEN_WIDTH >= 768,
  isAndroid: Platform.OS === 'android',
  isIOS: Platform.OS === 'ios',
};

export const CAMERA_MODES = {
  photo: {
    key: 'photo' as const,
    labelAr: 'صورة',
    labelEn: 'Photo',
    icon: 'camera-outline',
  },
  video: {
    key: 'video' as const,
    labelAr: 'فيلم',
    labelEn: 'Video',
    icon: 'videocamera-outline',
  },
  pro: {
    key: 'pro' as const,
    labelAr: 'احترافي',
    labelEn: 'Pro',
    icon: 'settings-outline',
  },
};

export const SCENE_TYPES: Record<SceneType, { labelAr: string; labelEn: string; icon: string }> = {
  portrait: { labelAr: 'بورتريه', labelEn: 'Portrait', icon: 'person-outline' },
  food: { labelAr: 'طعام', labelEn: 'Food', icon: 'restaurant-outline' },
  landscape: { labelAr: 'منظر طبيعي', labelEn: 'Landscape', icon: 'landscape-outline' },
  document: { labelAr: 'مستند', labelEn: 'Document', icon: 'document-outline' },
  night: { labelAr: 'ليلي', labelEn: 'Night', icon: 'moon-outline' },
  macro: { labelAr: 'macRO', labelEn: 'Macro', icon: 'search-outline' },
  action: { labelAr: 'حركة', labelEn: 'Action', icon: 'flash-outline' },
  unknown: { labelAr: 'غير معروف', labelEn: 'Unknown', icon: 'help-outline' },
};

export const DEFAULT_SETTINGS: AppSettings = {
  aiAssistantEnabled: true,
  autoSaveOriginal: true,
  quality: 'high',
  format: 'jpeg',
  gridEnabled: true,
  levelEnabled: true,
  hapticsEnabled: true,
  language: 'ar',
  theme: 'auto',
  shutterSound: true,
  countdown: 0,
};

export const SUPPORTED_VIDEO_RESOLUTIONS = [
  { label: '480p', value: '480p' },
  { label: '720p', value: '720p' },
  { label: '1080p', value: '1080p' },
  { label: '4K', value: '4K' },
];

export const SUPPORTED_FRAME_RATES = [24, 30, 60];