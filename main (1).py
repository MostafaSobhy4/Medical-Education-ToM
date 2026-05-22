import cv2
import numpy as np
import mediapipe as mp

# ============================================
# MEDIAPIPE INITIALIZATION
# ============================================

mp_face_mesh = mp.solutions.face_mesh

face_mesh = mp_face_mesh.FaceMesh(
    refine_landmarks=True,
    max_num_faces=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

mp_selfie_segmentation = mp.solutions.selfie_segmentation

selfie_segmentation = mp_selfie_segmentation.SelfieSegmentation(
    model_selection=1
)

# ============================================
# LANDMARKS
# ============================================

LEFT_EYE = [33, 133, 160, 159, 158, 153]
RIGHT_EYE = [362, 385, 387, 386, 374, 373]

# ============================================
# TEMPORAL SMOOTHING
# ============================================

previous_mask = None

# ============================================
# IMPROVED BUTTERFLY RASH REGIONS
# ============================================

LEFT_CHEEK = [

    50, 101, 118, 117, 116,
    123, 147, 187, 207, 206,
    205, 203, 142, 100

]

RIGHT_CHEEK = [

    280, 330, 347, 346, 345,
    352, 376, 411, 427, 426,
    425, 423, 371, 329

]

NOSE_BRIDGE = [

    168, 197, 195, 5, 4, 45,
    275, 440, 344, 278

]

# ============================================
# JAUNDICE FILTER
# ============================================

def apply_jaundice(frame, segmentation_mask, landmarks=None):

    global previous_mask

    h, w, _ = frame.shape

    # =====================================
    # PERSON MASK (FLOAT MASK)
    # =====================================

    person_mask = segmentation_mask.astype(np.float32)

    # smooth segmentation edges
    person_mask = cv2.GaussianBlur(person_mask, (21, 21), 0)

    # =====================================
    # HSV SKIN DETECTION
    # =====================================

    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

    lower_skin = np.array([0, 15, 60], dtype=np.uint8)
    upper_skin = np.array([35, 255, 255], dtype=np.uint8)

    skin_mask = cv2.inRange(hsv, lower_skin, upper_skin)

    # convert binary mask -> float
    skin_mask = skin_mask.astype(np.float32) / 255.0

    # =====================================
    # CLEAN SKIN MASK
    # =====================================

    kernel = cv2.getStructuringElement(
        cv2.MORPH_ELLIPSE,
        (7, 7)
    )

    skin_mask = cv2.erode(skin_mask, kernel, iterations=1)
    skin_mask = cv2.dilate(skin_mask, kernel, iterations=1)

    skin_mask = cv2.GaussianBlur(skin_mask, (11, 11), 0)

    # =====================================
    # COMBINE MASKS (SOFT COMBINATION)
    # =====================================

    combined_mask = person_mask * skin_mask

    combined_mask = np.clip(combined_mask, 0, 1)

    # =====================================
    # TEMPORAL SMOOTHING
    # =====================================

    if previous_mask is None:
        previous_mask = combined_mask.copy()

    combined_mask = cv2.addWeighted(
        combined_mask,
        0.7,
        previous_mask,
        0.3,
        0
    )

    previous_mask = combined_mask.copy()

    # =====================================
    # REALISTIC JAUNDICE COLOR SHIFT
    # =====================================

    hsv_tinted = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV).astype(np.float32)

    # slight hue shift toward yellow
    hsv_tinted[:, :, 0] += 8

    # slightly increase saturation
    hsv_tinted[:, :, 1] *= 1.5

    # slightly darken
    hsv_tinted[:, :, 2] *= 0.92

    hsv_tinted = np.clip(hsv_tinted, 0, 255).astype(np.uint8)

    tinted = cv2.cvtColor(hsv_tinted, cv2.COLOR_HSV2BGR)

    # =====================================
    # APPLY FILTER USING FLOAT MASK
    # =====================================

    combined_mask_3ch = cv2.merge([
        combined_mask,
        combined_mask,
        combined_mask
    ])

    result = (
        frame.astype(np.float32) * (1 - combined_mask_3ch)
        +
        tinted.astype(np.float32) * combined_mask_3ch
    )

    result = np.clip(result, 0, 255).astype(np.uint8)

    # =====================================
    # EXTRA EYE YELLOWING
    # =====================================

    if landmarks is not None:

        eye_overlay = result.copy()

        for idx in LEFT_EYE + RIGHT_EYE:

            x = int(landmarks[idx].x * w)
            y = int(landmarks[idx].y * h)

            # cv2.circle(
            #     eye_overlay,
            #     (x, y),
            #     3,
            #     (0, 255, 255),
            #     -1
            # )

        result = cv2.addWeighted(
            result,
            0.85,
            eye_overlay,
            0.15,
            0
        )

    # =====================================
    # DEBUG WINDOW (OPTIONAL)
    # =====================================

    debug_mask = (combined_mask * 255).astype(np.uint8)

    cv2.imshow("Skin Mask", debug_mask)

    return result

# ============================================
# EDEMA FILTER
# ============================================

def apply_edema(frame, landmarks):

    h, w, _ = frame.shape

    for eye in [LEFT_EYE, RIGHT_EYE]:

        pts = []

        for idx in eye:

            x = int(landmarks[idx].x * w)
            y = int(landmarks[idx].y * h)

            pts.append([x, y])

        pts = np.array(pts, np.int32)

        x, y, bw, bh = cv2.boundingRect(pts)

        x -= 10
        y -= 10
        bw += 20
        bh += 20

        # SAFE CLAMPING
        x = max(0, x)
        y = max(0, y)

        x2 = min(w, x + bw)
        y2 = min(h, y + bh)

        roi = frame[y:y2, x:x2]

        if roi.size == 0:
            continue

        blurred = cv2.GaussianBlur(
            roi,
            (35, 35),
            30
        )

        frame[y:y2, x:x2] = cv2.addWeighted(
            roi,
            0.3,
            blurred,
            0.7,
            0
        )

    return frame

def apply_butterfly_rash(frame, landmarks):

    h, w, _ = frame.shape

    # =====================================
    # CREATE EMPTY MASK
    # =====================================

    mask = np.zeros((h, w), dtype=np.uint8)

    # =====================================
    # LEFT CHEEK
    # =====================================

    left_pts = []

    for idx in LEFT_CHEEK:

        x = int(landmarks[idx].x * w)
        y = int(landmarks[idx].y * h)

        left_pts.append([x, y])

    left_pts = np.array(left_pts, np.int32)

    cv2.fillPoly(mask, [left_pts], 255)

    # =====================================
    # RIGHT CHEEK
    # =====================================

    right_pts = []

    for idx in RIGHT_CHEEK:

        x = int(landmarks[idx].x * w)
        y = int(landmarks[idx].y * h)

        right_pts.append([x, y])

    right_pts = np.array(right_pts, np.int32)

    cv2.fillPoly(mask, [right_pts], 255)

    # =====================================
    # NOSE BRIDGE
    # =====================================

    nose_pts = []

    for idx in NOSE_BRIDGE:

        x = int(landmarks[idx].x * w)
        y = int(landmarks[idx].y * h)

        nose_pts.append([x, y])

    nose_pts = np.array(nose_pts, np.int32)

    cv2.fillPoly(mask, [nose_pts], 255)

    # =====================================
    # SOFTEN MASK
    # =====================================

    # multi-stage blur for soft medical appearance

    mask = cv2.GaussianBlur(mask, (31, 31), 0)

    mask = cv2.GaussianBlur(mask, (51, 51), 0)

    mask = cv2.GaussianBlur(mask, (71, 71), 0)

    mask_float = mask.astype(np.float32) / 255.0

    mask_3ch = cv2.merge([
        mask_float,
        mask_float,
        mask_float
    ])

    # =====================================
    # CREATE RASH COLOR
    # =====================================

    rash = frame.copy().astype(np.float32)

    # deep red inflammation look

    # strong red boost
    rash[:, :, 2] *= 1.75

    # suppress green
    rash[:, :, 1] *= 0.78

    # suppress blue
    rash[:, :, 0] *= 0.72

    rash *= 0.92

    rash = np.clip(rash, 0, 255).astype(np.uint8)

    noise = np.random.normal(
    0,
    8,
    frame.shape
    ).astype(np.float32)

    rash = rash.astype(np.float32) + noise

    rash = np.clip(rash, 0, 255).astype(np.uint8)

    # =====================================
    # BLEND
    # =====================================

    opacity = 0.65

    result = (
        frame.astype(np.float32) * (1 - mask_3ch * opacity)
        +
        rash.astype(np.float32) * (mask_3ch * opacity)
    )

    result = np.clip(result, 0, 255).astype(np.uint8)

    return result

# ============================================
# MAIN
# ============================================

def main():

    print("Choose filter:")
    print("1 -> Jaundice")
    print("2 -> Edema")
    print("3 -> Butterfly Rash")

    choice = input("Enter 1 or 2 or 3: ")

    cap = cv2.VideoCapture(0)

    # HIGHER CAMERA RESOLUTION
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    while True:

        ret, frame = cap.read()

        if not ret:
            break

        # mirror webcam
        frame = cv2.flip(frame, 1)

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        # =====================================
        # FACE MESH
        # =====================================

        face_result = face_mesh.process(rgb)

        # =====================================
        # SELFIE SEGMENTATION
        # =====================================

        segmentation_result = selfie_segmentation.process(rgb)

        segmentation_mask = segmentation_result.segmentation_mask

        # =====================================
        # APPLY FILTERS
        # =====================================

        landmarks = None

        if face_result.multi_face_landmarks:

            landmarks = (
                face_result
                .multi_face_landmarks[0]
                .landmark
            )

        if choice == "1":

            frame = apply_jaundice(
                frame,
                segmentation_mask,
                landmarks
            )

        elif choice == "2":

            if landmarks is not None:

                frame = apply_edema(
                    frame,
                    landmarks
                )
        
        elif choice == "3":

            if landmarks is not None:

                frame = apply_butterfly_rash(
                    frame,
                    landmarks
                )

        # =====================================
        # DISPLAY
        # =====================================

        cv2.imshow(
            "AR Medical Filter",
            frame
        )

        # PRESS Q TO EXIT
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()

    cv2.destroyAllWindows()

# ============================================
# ENTRY POINT
# ============================================

if __name__ == "__main__":
    main()