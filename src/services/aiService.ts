/**
 * AI service - scene detection logic
 */

import { SceneType, AiSuggestion, SceneAnalysis, CameraSettings } from '../types';

export const analyzeImageForScene = (imageUri: string): Promise<SceneAnalysis> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const scenes: SceneType[] = [
        'portrait', 'food', 'landscape', 'document', 'night', 'macro', 'action', 'unknown',
      ];
      const sceneType = scenes[Math.floor(Math.random() * scenes.length)];
      const confidence = 0.6 + Math.random() * 0.4;

      const suggestions: AiSuggestion[] = [
        {
          id: 'ai-angle',
          type: 'angle',
          priority: 'medium',
          message: 'جرب التصوير من مستوى أقل',
          messageAr: 'Try shooting from a lower angle',
          icon: 'arrow-down-outline',
          actionable: true,
        },
        {
          id: 'ai-light',
          type: 'light',
          priority: 'medium',
          message: 'اجعل مصدر الضوء أمامك',
          messageAr: 'Place light source in front of you',
          icon: 'sun-outline',
          actionable: true,
        },
        {
          id: 'ai-stability',
          type: 'stability',
          priority: 'high',
          message: 'ثبت الهاتف لتجنب الاهتزاز',
          messageAr: 'Hold phone steady to avoid shake',
          icon: 'alert-circle-outline',
          actionable: false,
        },
      ];

      resolve({
        sceneType,
        confidence,
        suggestedSettings: {
          iso: 100,
          shutterSpeed: 1 / 60,
          focusDistance: 1,
          whiteBalance: 'auto',
          exposure: 0,
          zoom: 1,
        },
        suggestions,
        detectedFaces: sceneType === 'portrait' ? 1 : undefined,
        isStable: Math.random() > 0.3,
        brightness: 0.5,
        qualityScore: 0.75,
      });
    }, 800);
  });
};

export const generateAutoSettings = (scene: SceneType): CameraSettings => {
  const presets: Record<SceneType, CameraSettings> = {
    portrait: { iso: 100, shutterSpeed: 1 / 125, focusDistance: 0.5, whiteBalance: 'auto', exposure: 0, zoom: 1 },
    food: { iso: 200, shutterSpeed: 1 / 60, focusDistance: 0.3, whiteBalance: 'daylight', exposure: 0.1, zoom: 1 },
    landscape: { iso: 100, shutterSpeed: 1 / 60, focusDistance: 100, whiteBalance: 'daylight', exposure: 0, zoom: 1 },
    document: { iso: 100, shutterSpeed: 1 / 125, focusDistance: 0.3, whiteBalance: 'fluorescent', exposure: 0, zoom: 1 },
    night: { iso: 800, shutterSpeed: 1 / 15, focusDistance: 100, whiteBalance: 'tungsten', exposure: 0.5, zoom: 1 },
    macro: { iso: 200, shutterSpeed: 1 / 60, focusDistance: 0.1, whiteBalance: 'auto', exposure: 0, zoom: 1 },
    action: { iso: 400, shutterSpeed: 1 / 500, focusDistance: 5, whiteBalance: 'auto', exposure: 0, zoom: 1 },
    unknown: { iso: 100, shutterSpeed: 1 / 60, focusDistance: 1, whiteBalance: 'auto', exposure: 0, zoom: 1 },
  };
  return presets[scene];
};