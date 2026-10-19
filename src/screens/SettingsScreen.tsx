/**
 * Settings Screen - full settings
 */

import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Switch } from 'react-native';
import { useApp } from '../context/AppContext';
import { COLORS, SPACING, BORDER_RADIUS } from '../constants/theme';

export const SettingsScreen = ({ navigation }: any) => {
  const { settings, updateSettings } = useApp();

  const SettingRow = ({ label, value, onToggle }: any) => (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Switch
        value={value}
        onValueChange={onToggle}
        trackColor={{ false: COLORS.border, true: COLORS.primary }}
        thumbColor={COLORS.text.primary}
      />
    </View>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>الإعدادات</Text>
      </View>
      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>الذكاء الاصطناعي</Text>
          <SettingRow
            label="مساعد AI"
            value={settings.aiAssistantEnabled}
            onToggle={(v: boolean) => updateSettings({ aiAssistantEnabled: v })}
          />
        </View>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>الكاميرا</Text>
          <SettingRow
            label="شبكة التكوين"
            value={settings.gridEnabled}
            onToggle={(v: boolean) => updateSettings({ gridEnabled: v })}
          />
          <SettingRow
            label="المستوى الأفقي"
            value={settings.levelEnabled}
            onToggle={(v: boolean) => updateSettings({ levelEnabled: v })}
          />
          <SettingRow
            label="اهتزاز اللمس"
            value={settings.hapticsEnabled}
            onToggle={(v: boolean) => updateSettings({ hapticsEnabled: v })}
          />
          <SettingRow
            label="صوت الغالق"
            value={settings.shutterSound}
            onToggle={(v: boolean) => updateSettings({ shutterSound: v })}
          />
        </View>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>الجودة</Text>
          <View style={styles.row}>
            <Text style={styles.rowLabel}>جودة الصورة</Text>
            <Text style={styles.rowValue}>{settings.quality}</Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.rowLabel}>التنسيق</Text>
            <Text style={styles.rowValue}>{settings.format}</Text>
          </View>
        </View>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>اللغة</Text>
          <View style={styles.row}>
            <Text style={styles.rowLabel}>اللغة</Text>
            <Text style={styles.rowValue}>{settings.language === 'ar' ? 'العربية' : 'English'}</Text>
          </View>
        </View>
      </ScrollView>
      <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
        <Text style={styles.backButtonText}>← رجوع</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background },
  header: { paddingTop: 60, paddingHorizontal: SPACING.md, paddingBottom: SPACING.md },
  title: { fontSize: 28, fontWeight: '700', color: COLORS.text.primary, textAlign: 'right' },
  content: { flex: 1, paddingHorizontal: SPACING.md },
  section: { marginBottom: SPACING.xl },
  sectionTitle: { color: COLORS.primary, fontSize: 14, fontWeight: '600', marginBottom: SPACING.sm, textAlign: 'right' },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: SPACING.md, borderBottomWidth: 1, borderBottomColor: COLORS.borderLight },
  rowLabel: { color: COLORS.text.primary, fontSize: 16, textAlign: 'right' },
  rowValue: { color: COLORS.text.secondary, fontSize: 14 },
  backButton: { margin: SPACING.md, padding: SPACING.md, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, alignItems: 'center' },
  backButtonText: { color: COLORS.text.primary, fontSize: 16, fontWeight: '600' },
});
