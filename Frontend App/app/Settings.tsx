import { Isloggedin } from "@/context/IsLoggedIn";
import { Ionicons } from "@expo/vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { router } from "expo-router";
import { useContext } from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function Settings() {

    const { isLoggedin, setIsLoggedin } = useContext(Isloggedin);

  const handleLogout = async () => {
    await AsyncStorage.removeItem("currentUser");
    router.replace("/Login");
  };

  const clearData = async () => {
    await AsyncStorage.clear();
    alert("Data cleared Successfully");
  };

  return (
    <View style={styles.container}>

      <Text style={styles.title}>Settings</Text>

        {isLoggedin ? (
          <TouchableOpacity style={styles.item} onPress={() => router.push("/Profile")}>
            <Ionicons name="person-outline" size={22} color="white" />
            <Text style={styles.text}>Profile</Text>
          </TouchableOpacity>
        ) : (
          <></>
        )}

      <TouchableOpacity style={styles.item} onPress={clearData}>
        <Ionicons name="trash-outline" size={22} color="white" />
        <Text style={styles.text}>Clear Data</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.item} onPress={() => router.push("/About App")}>
        <Ionicons name="information-circle-outline" size={22} color="white" />
        <Text style={styles.text}>About App</Text>
      </TouchableOpacity>


        {isLoggedin ? (
            <TouchableOpacity style={styles.logout} onPress={handleLogout}>
                <Ionicons name="log-out-outline" size={22} color="white" />
                <Text style={styles.text}>Logout</Text>
            </TouchableOpacity>
        ) : (
          <></>
        )}
 

    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#0F1115",
    padding: 20,
    gap: 20,
  },

  title: {
    color: "white",
    fontSize: 26,
    fontWeight: "bold",
    marginBottom: 20,
  },

  item: {
    flexDirection: "row",
    alignItems: "center",
    gap: 15,
    padding: 15,
    backgroundColor: "#1E1E1E",
    borderRadius: 12,
  },

  logout: {
    flexDirection: "row",
    alignItems: "center",
    gap: 15,
    padding: 15,
    backgroundColor: "#D63031",
    borderRadius: 12,
    marginTop: 20,
  },

  text: {
    color: "white",
    fontSize: 16,
  },
});