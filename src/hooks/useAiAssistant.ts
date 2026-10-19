/**
 * AI Assistant hook - scene analysis and suggestions
 */

import { useState, useCallback, useMemo } from 'react';
import { SceneType, AiSuggestion, SceneAnalysis, CameraSettings } from '../types';
import { SCENE_TYPES } from '../constants/theme';

export const useAiAssistant = () => {
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [lastAnalysis, setLastAnalysis] = useState<SceneAnalysis | null>(null);

  const analyzeScene = useCallback((frameData: any): SceneAnalysis => {
    // Mock scene analysis - in production, integrate with ML model
    const sceneTypes: SceneType[] = [
      'portrait', 'food', 'landscape', 'document', 'night', 'macro', 'action',
    ];
    const randomScene = sceneTypes[Math.floor(Math.random() * sceneTypes.length)];
    const confidence = 0.65 + Math.random() * 0.3;

    const suggestions: AiSuggestion[] = [
      {
        id: 's1',
        type: 'angle',
        priority: 'medium',
        message: 'جرب التصوير من مستوى أقل',
        messageAr: 'Try shooting from a lower angle',
        icon: 'arrow-down-outline',
        actionable: true,
      },
      {
        id: 's2',
        type: 'distance',
        priority: 'low',
        message: 'اقترب قليلاً من الموضوع',
        messageAr: 'Get closer to the subject',
        icon: 'arrow-inward-outline',
        actionable: true,
      },
      {
        id: 's3',
        type: 'light',
        priority: 'medium',
        message: 'اجعل مصدر الضوء أمامك',
        messageAr: 'Place light source in front of you',
        icon: 'sun-outline',
        actionable: true,
      },
      {
        id: 's4',
        type: 'composition',
        priority: 'low',
        message: 'استخدم قاعدة الأثلاث',
        messageAr: 'Use the rule of thirds grid',
        icon: 'grid-outline',
        actionable: true,
      },
      {
        id: 's5',
        type: 'stability',
        priority: 'high',
        message: 'ثبت الهاتف لتجنب الاهتزاز',
        messageAr: 'Hold phone steady to avoid shake',
        icon: 'alert-circle-outline',
        actionable: false,
      },
    ];

    const sceneConfig = SCENE_TYPES[randomScene];
    const suggestedSettings: CameraSettings = {
      iso: 100,
      shutterSpeed: 1 / 60,
      focusDistance: 1,
      whiteBalance: 'auto',
      exposure: 0,
      zoom: 1,
    };

    const analysis: SceneAnalysis = {
      sceneType: randomScene,
      confidence,
      suggestedSettings,
      suggestions,
      detectedFaces: randomScene === 'portrait' ? Math.floor(Math.random() * 3) + 1 : undefined,
      isStable: Math.random() > 0.3,
      brightness: 0.4 + Math.random() * 0.4,
      qualityScore: 0.7 + Math.random() * 0.3,
    };

    setLastAnalysis(analysis);
    return analysis;
  }, []);

  const clearAnalysis = useCallback(() => {
    setLastAnalysis(null);
  }, []);

  return {
    isAnalyzing,
    lastAnalysis,
    analyzeScene,
    clearAnalysis,
  };
};

export default useAiAssistant;