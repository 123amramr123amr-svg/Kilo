/**
 * Navigation setup for LensAI
 */

import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/native';
import React from 'react';
import {
  CameraScreen,
  GalleryScreen,
  PhotoDetailScreen,
  PhotoEditorScreen,
  SettingsScreen,
  AiSuggestionsScreen,
  PermissionsScreen,
} from '../screens';

const Stack = createStackNavigator();

export const AppNavigator = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
          presentation: 'modal',
        }}
      >
        <Stack.Screen name="Permissions" component={PermissionsScreen} />
        <Stack.Screen name="Camera" component={CameraScreen} />
        <Stack.Screen name="Gallery" component={GalleryScreen} />
        <Stack.Screen name="PhotoDetail" component={PhotoDetailScreen} />
        <Stack.Screen name="PhotoEditor" component={PhotoEditorScreen} />
        <Stack.Screen name="Settings" component={SettingsScreen} />
        <Stack.Screen name="AiSuggestions" component={AiSuggestionsScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
};