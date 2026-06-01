import AsyncStorage from "@react-native-async-storage/async-storage";
import { router } from "expo-router";
import { useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

export default function Profile() {
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    loadUser();
  }, []);

  const loadUser = async () => {
    const Accounts = await AsyncStorage.getItem("Accounts");

    if (Accounts) {
      const parsedAccounts = JSON.parse(Accounts);
      const currentUser = parsedAccounts.find((acc: any) => acc.isCurrent);

      if (currentUser) {
        setUser(currentUser);
      }
    }
  };

  const handleLogout = async () => {
    const Accounts = await AsyncStorage.getItem("Accounts");

    if (Accounts) {
      let parsedAccounts = JSON.parse(Accounts);

      parsedAccounts = parsedAccounts.map((acc: any) => ({
        ...acc,
        isCurrent: false,
      }));

      await AsyncStorage.setItem("Accounts", JSON.stringify(parsedAccounts));
    }

    router.replace("/Login");
  };

  if (!user) {
    return (
      <View style={styles.container}>
        <Text style={{ color: "white" }}>Loading...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Profile</Text>

      <View style={styles.card}>
        <Text style={styles.label}>Name</Text>
        <Text style={styles.value}>{user.name}</Text>

        <Text style={styles.label}>Username</Text>
        <Text style={styles.value}>{user.username}</Text>

        <Text style={styles.label}>Email</Text>
        <Text style={styles.value}>{user.email}</Text>

        <Text style={styles.label}>Phone</Text>
        <Text style={styles.value}>{user.phone}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#0F1115",
    padding: 20,
    alignItems: "center",
  },

  title: {
    color: "white",
    fontSize: 24,
    marginBottom: 30,
  },

  card: {
    width: "100%",
    backgroundColor: "#191d25",
    padding: 20,
    borderRadius: 12,
    gap: 10,
  },

  label: {
    color: "#aaa",
    fontSize: 12,
  },

  value: {
    color: "white",
    fontSize: 16,
    marginBottom: 10,
  },
});