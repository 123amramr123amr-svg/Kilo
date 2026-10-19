/**
 * AI Suggestions Screen - log of AI suggestions
 */

import React from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { useApp } from '../context/AppContext';
import { COLORS, SPACING, BORDER_RADIUS } from '../constants/theme';

export const AiSuggestionsScreen = ({ route, navigation }: any) => {
  const { photoId } = route.params || {};
  const { aiSessions } = useApp();

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>سجل اقتراحات AI</Text>
      </View>
      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {aiSessions.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyIcon}>✨</Text>
            <Text style={styles.emptyText}>لا توجد اقتراحات بعد</Text>
            <Text style={styles.emptySubtext}>استخدم مساعد AI أثناء التصوير للحصول على اقتراحات</Text>
          </View>
        ) : (
          aiSessions.map(session => (
            <View key={session.id} style={styles.sessionCard}>
              <Text style={styles.sessionTitle}>جلسة #{session.id.slice(-6)}</Text>
              <Text style={styles.sessionDate}>{new Date(session.timestamp).toLocaleString('ar-EG')}</Text>
              {session.suggestions.map(s => (
                <View key={s.id} style={styles.suggestionItem}>
                  <Text style={styles.suggestionIcon}>{s.icon}</Text>
                  <Text style={styles.suggestionText}>{s.message}</Text>
                </View>
              ))}
            </View>
          ))
        )}
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
  emptyState: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingTop: 100 },
  emptyIcon: { fontSize: 64, marginBottom: SPACING.md },
  emptyText: { color: COLORS.text.primary, fontSize: 20, fontWeight: '600', marginBottom: SPACING.xs },
  emptySubtext: { color: COLORS.text.secondary, fontSize: 14, textAlign: 'center' },
  sessionCard: { backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, padding: SPACING.md, marginBottom: SPACING.md },
  sessionTitle: { color: COLORS.text.primary, fontSize: 16, fontWeight: '600', marginBottom: SPACING.xs, textAlign: 'right' },
  sessionDate: { color: COLORS.text.tertiary, fontSize: 12, marginBottom: SPACING.md, textAlign: 'right' },
  suggestionItem: { flexDirection: 'row', alignItems: 'center', gap: SPACING.sm, paddingVertical: SPACING.xs },
  suggestionIcon: { fontSize: 16 },
  suggestionText: { color: COLORS.text.secondary, fontSize: 14, flex: 1, textAlign: 'right' },
  backButton: { margin: SPACING.md, padding: SPACING.md, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, alignItems: 'center' },
  backButtonText: { color: COLORS.text.primary, fontSize: 16, fontWeight: '600' },
});
